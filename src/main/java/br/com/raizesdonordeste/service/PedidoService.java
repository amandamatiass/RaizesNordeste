package br.com.raizesdonordeste.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.raizesdonordeste.domain.CardapioUnidade;
import br.com.raizesdonordeste.domain.Cliente;
import br.com.raizesdonordeste.domain.Estoque;
import br.com.raizesdonordeste.domain.HistoricoStatus;
import br.com.raizesdonordeste.domain.ItemPedido;
import br.com.raizesdonordeste.domain.MovimentacaoEstoque;
import br.com.raizesdonordeste.domain.Pedido;
import br.com.raizesdonordeste.domain.StatusPagamento;
import br.com.raizesdonordeste.domain.StatusPedido;
import br.com.raizesdonordeste.domain.TipoMovimentacao;
import br.com.raizesdonordeste.domain.Unidade;
import br.com.raizesdonordeste.domain.Usuario;
import br.com.raizesdonordeste.exception.ConflitoException;
import br.com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.exception.RegraNegocioException;
import br.com.raizesdonordeste.repository.CardapioUnidadeRepository;
import br.com.raizesdonordeste.repository.ClienteRepository;
import br.com.raizesdonordeste.repository.EstoqueRepository;
import br.com.raizesdonordeste.repository.HistoricoStatusRepository;
import br.com.raizesdonordeste.repository.MovimentacaoEstoqueRepository;
import br.com.raizesdonordeste.repository.PagamentoRepository;
import br.com.raizesdonordeste.repository.PedidoRepository;
import br.com.raizesdonordeste.repository.UnidadeRepository;
import br.com.raizesdonordeste.repository.UsuarioRepository;
import br.com.raizesdonordeste.security.Acesso;
import br.com.raizesdonordeste.security.AuthUser;
import br.com.raizesdonordeste.web.dto.PedidoDtos.CriarPedidoRequest;
import br.com.raizesdonordeste.web.dto.PedidoDtos.HistoricoStatusResponse;
import br.com.raizesdonordeste.web.dto.PedidoDtos.ItemPedidoRequest;
import br.com.raizesdonordeste.web.dto.PedidoDtos.PedidoResponse;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final UnidadeRepository unidadeRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final CardapioUnidadeRepository cardapioRepository;
    private final EstoqueRepository estoqueRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final HistoricoStatusRepository historicoStatusRepository;
    private final PagamentoRepository pagamentoRepository;
    private final HistoricoService historico;
    private final FidelidadeService fidelidadeService;

    public PedidoService(PedidoRepository pedidoRepository, UnidadeRepository unidadeRepository,
                         ClienteRepository clienteRepository, UsuarioRepository usuarioRepository,
                         CardapioUnidadeRepository cardapioRepository, EstoqueRepository estoqueRepository,
                         MovimentacaoEstoqueRepository movimentacaoRepository,
                         HistoricoStatusRepository historicoStatusRepository,
                         PagamentoRepository pagamentoRepository, HistoricoService historico,
                         FidelidadeService fidelidadeService) {
        this.pedidoRepository = pedidoRepository;
        this.unidadeRepository = unidadeRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.cardapioRepository = cardapioRepository;
        this.estoqueRepository = estoqueRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.historicoStatusRepository = historicoStatusRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.historico = historico;
        this.fidelidadeService = fidelidadeService;
    }

    // ------------------------------------------------------------------ criação

    /**
     * Valida produtos, quantidades, preços vigentes e disponibilidade na unidade; reserva o estoque e registra o
     * pedido com os preços praticados (que não mudam se o cardápio for alterado depois).
     * Se a chave de idempotência já foi usada, devolve o pedido existente sem criar outro.
     */
    @Transactional
    public PedidoResponse criar(AuthUser user, String chaveIdempotencia, CriarPedidoRequest req) {
        String chave = (chaveIdempotencia == null || chaveIdempotencia.isBlank()) ? null : chaveIdempotencia.trim();
        if (chave != null) {
            Optional<Pedido> existente = pedidoRepository.findByChaveIdempotencia(chave);
            if (existente.isPresent()) {
                Acesso.exigirAcessoPedido(user, existente.get());
                return PedidoResponse.de(existente.get());
            }
        }

        Unidade unidade = unidadeRepository.findById(req.unidadeId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade não encontrada: " + req.unidadeId()));
        if (!unidade.isAtiva()) {
            throw new RegraNegocioException("Unidade inativa");
        }

        Cliente cliente = null;
        Usuario operador = null;
        if (Acesso.ehCliente(user)) {
            cliente = clienteRepository.findById(user.id())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));
        } else {
            Acesso.exigirUnidade(user, unidade.getId());
            operador = usuarioRepository.findById(user.id()).orElse(null);
            if (req.clienteId() != null) {
                cliente = clienteRepository.findById(req.clienteId()).orElseThrow(
                        () -> new RecursoNaoEncontradoException("Cliente não encontrado: " + req.clienteId()));
            }
        }

        Map<Long, Integer> quantidades = new LinkedHashMap<>();
        for (ItemPedidoRequest item : req.itens()) {
            quantidades.merge(item.produtoId(), item.quantidade(), Integer::sum);
        }

        boolean antecipado = req.pagamentoAntecipado() != null
                ? req.pagamentoAntecipado()
                : req.canal().exigePagamentoAntecipadoPorPadrao();

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setUnidade(unidade);
        pedido.setCanal(req.canal());
        pedido.setPagamentoAntecipado(antecipado);
        pedido.setStatus(antecipado ? StatusPedido.AGUARDANDO_PAGAMENTO : StatusPedido.RECEBIDO);
        pedido.setCriadoEm(LocalDateTime.now());
        pedido.setChaveIdempotencia(chave);
        pedido.setValorDesconto(BigDecimal.ZERO);

        LocalDate hoje = LocalDate.now();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entrada : quantidades.entrySet()) {
            Long produtoId = entrada.getKey();
            CardapioUnidade cardapio = cardapioRepository.findByUnidadeIdAndProdutoId(unidade.getId(), produtoId)
                    .filter(c -> c.estaDisponivel(hoje))
                    .orElseThrow(() -> new RegraNegocioException(
                            "Produto " + produtoId + " indisponível na unidade " + unidade.getId()));
            ItemPedido item = new ItemPedido();
            item.setProduto(cardapio.getProduto());
            item.setQuantidade(entrada.getValue());
            item.setPrecoUnitario(cardapio.getPreco());
            pedido.adicionarItem(item);
            total = total.add(item.subtotal());
        }
        pedido.setValorTotal(total);
        Pedido salvo = pedidoRepository.save(pedido);

        for (ItemPedido item : salvo.getItens()) {
            Estoque estoque = estoqueRepository
                    .findByUnidadeIdAndProdutoId(unidade.getId(), item.getProduto().getId())
                    .orElseThrow(() -> new RegraNegocioException(
                            "Sem estoque cadastrado para o produto " + item.getProduto().getNome()));
            int linhas = estoqueRepository.reservar(estoque.getId(), item.getQuantidade());
            if (linhas == 0) {
                // A exceção desfaz a transação inteira: pedido e reservas anteriores são revertidos.
                throw new RegraNegocioException("Estoque insuficiente para o produto " + item.getProduto().getNome());
            }
            registrarMovimentacao(estoque, operador, salvo, TipoMovimentacao.RESERVA, item.getQuantidade(),
                    "Reserva do pedido");
        }
        registrarTransicao(salvo, null, salvo.getStatus(), operador);
        return PedidoResponse.de(salvo);
    }

    // ------------------------------------------------------------------ consulta

    @Transactional(readOnly = true)
    public PedidoResponse buscar(AuthUser user, Long id) {
        Pedido pedido = carregar(id);
        Acesso.exigirAcessoPedido(user, pedido);
        return PedidoResponse.de(pedido);
    }

    /** Fila de pedidos. Equipe da unidade enxerga só a própria; a matriz pode consultar qualquer unidade. */
    @Transactional(readOnly = true)
    public List<PedidoResponse> listar(AuthUser user, Long unidadeId, StatusPedido status, int page, int size) {
        Long unidade = unidadeId;
        if (!Acesso.ehAdmin(user)) {
            if (unidade == null) {
                unidade = user.unidadeId();
            }
            Acesso.exigirUnidade(user, unidade);
        }
        PageRequest pagina = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        List<Pedido> pedidos;
        if (unidade == null) {
            pedidos = status == null
                    ? pedidoRepository.findAllByOrderByCriadoEmDesc(pagina)
                    : pedidoRepository.findByStatusOrderByCriadoEmDesc(status, pagina);
        } else {
            pedidos = status == null
                    ? pedidoRepository.findByUnidadeIdOrderByCriadoEmDesc(unidade, pagina)
                    : pedidoRepository.findByUnidadeIdAndStatusOrderByCriadoEmDesc(unidade, status, pagina);
        }
        return pedidos.stream().map(PedidoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<HistoricoStatusResponse> historicoStatus(AuthUser user, Long id) {
        Pedido pedido = carregar(id);
        Acesso.exigirAcessoPedido(user, pedido);
        return historicoStatusRepository.findByPedidoIdOrderByDataHoraAscIdAsc(id).stream()
                .map(HistoricoStatusResponse::de).toList();
    }

    // ------------------------------------------------------------------ andamento

    /** Avança o pedido pela sequência válida. Cancelamento e liberação por pagamento têm fluxos próprios. */
    @Transactional
    public PedidoResponse atualizarStatus(AuthUser user, Long id, StatusPedido novo) {
        Pedido pedido = carregar(id);
        Acesso.exigirUnidade(user, pedido.getUnidade().getId());
        if (novo == StatusPedido.CANCELADO) {
            throw new RegraNegocioException("Para cancelar, use POST /api/v1/pedidos/{id}/cancelamento");
        }
        if (pedido.getStatus() == StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new RegraNegocioException(
                    "Pedido aguardando pagamento aprovado: não pode avançar para preparação");
        }
        StatusPedido anterior = pedido.getStatus();
        pedido.mudarStatus(novo);
        Usuario operador = usuarioRepository.findById(user.id()).orElse(null);
        registrarTransicao(pedido, anterior, novo, operador);
        if (novo == StatusPedido.CONCLUIDO) {
            baixarEstoque(pedido, operador);
            fidelidadeService.acumularPontos(pedido);
        }
        return PedidoResponse.de(pedido);
    }

    @Transactional
    public PedidoResponse cancelar(AuthUser user, Long id, String motivo) {
        Pedido pedido = carregar(id);
        Acesso.exigirUnidade(user, pedido.getUnidade().getId());
        StatusPedido anterior = pedido.getStatus();
        pedido.mudarStatus(StatusPedido.CANCELADO);

        Usuario operador = usuarioRepository.findById(user.id()).orElse(null);
        for (ItemPedido item : pedido.getItens()) {
            Estoque estoque = estoqueRepository
                    .findByUnidadeIdAndProdutoId(pedido.getUnidade().getId(), item.getProduto().getId())
                    .orElseThrow(() -> new IllegalStateException("Estoque do produto não encontrado"));
            if (estoqueRepository.liberar(estoque.getId(), item.getQuantidade()) == 0) {
                throw new IllegalStateException("Reserva de estoque inconsistente para o pedido " + pedido.getId());
            }
            registrarMovimentacao(estoque, operador, pedido, TipoMovimentacao.LIBERACAO, item.getQuantidade(),
                    "Cancelamento do pedido");
        }
        registrarTransicao(pedido, anterior, StatusPedido.CANCELADO, operador);
        historico.registrar(user, "CANCELAMENTO_PEDIDO", "pedido", pedido.getId(),
                HistoricoService.dados("status", anterior.name(), "valorTotal", pedido.getValorTotal()),
                HistoricoService.dados("status", StatusPedido.CANCELADO.name(), "motivo", motivo));
        return PedidoResponse.de(pedido);
    }

    /** Desconto sobre o valor do pedido. Só antes de o pagamento ser iniciado e com pedido em andamento. */
    @Transactional
    public PedidoResponse conceder(AuthUser user, Long id, BigDecimal valor, String motivo) {
        Pedido pedido = carregar(id);
        Acesso.exigirUnidade(user, pedido.getUnidade().getId());
        if (pedido.getStatus().isFinal()) {
            throw new ConflitoException("Pedido encerrado: não é possível conceder desconto");
        }
        if (pagamentoRepository.existsByPedidoIdAndStatusIn(pedido.getId(),
                List.of(StatusPagamento.PENDENTE, StatusPagamento.APROVADO))) {
            throw new RegraNegocioException("O desconto deve ser concedido antes de iniciar o pagamento");
        }
        if (valor.compareTo(pedido.getValorTotal()) >= 0) {
            throw new RegraNegocioException("O desconto deve ser menor que o valor atual do pedido");
        }
        BigDecimal descontoAntes = pedido.getValorDesconto();
        BigDecimal totalAntes = pedido.getValorTotal();
        pedido.setValorDesconto(descontoAntes.add(valor));
        pedido.setValorTotal(totalAntes.subtract(valor));
        historico.registrar(user, "CONCESSAO_DESCONTO", "pedido", pedido.getId(),
                HistoricoService.dados("valorTotal", totalAntes, "valorDesconto", descontoAntes),
                HistoricoService.dados("valorTotal", pedido.getValorTotal(),
                        "valorDesconto", pedido.getValorDesconto(), "motivo", motivo));
        return PedidoResponse.de(pedido);
    }

    // ------------------------------------------------------------------ apoio

    private Pedido carregar(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado: " + id));
    }

    private void baixarEstoque(Pedido pedido, Usuario operador) {
        for (ItemPedido item : pedido.getItens()) {
            Estoque estoque = estoqueRepository
                    .findByUnidadeIdAndProdutoId(pedido.getUnidade().getId(), item.getProduto().getId())
                    .orElseThrow(() -> new IllegalStateException("Estoque do produto não encontrado"));
            if (estoqueRepository.baixar(estoque.getId(), item.getQuantidade()) == 0) {
                throw new IllegalStateException("Reserva de estoque inconsistente para o pedido " + pedido.getId());
            }
            registrarMovimentacao(estoque, operador, pedido, TipoMovimentacao.SAIDA, item.getQuantidade(),
                    "Pedido concluído");
        }
    }

    private void registrarMovimentacao(Estoque estoque, Usuario operador, Pedido pedido, TipoMovimentacao tipo,
                                       int quantidade, String motivo) {
        MovimentacaoEstoque mov = new MovimentacaoEstoque();
        mov.setEstoque(estoque);
        mov.setUsuario(operador);
        mov.setPedido(pedido);
        mov.setTipo(tipo);
        mov.setQuantidade(quantidade);
        mov.setMotivo(motivo);
        mov.setDataHora(LocalDateTime.now());
        movimentacaoRepository.save(mov);
    }

    private void registrarTransicao(Pedido pedido, StatusPedido anterior, StatusPedido novo, Usuario operador) {
        HistoricoStatus h = new HistoricoStatus();
        h.setPedido(pedido);
        h.setUsuario(operador);
        h.setStatusAnterior(anterior);
        h.setStatusNovo(novo);
        h.setDataHora(LocalDateTime.now());
        historicoStatusRepository.save(h);
    }
}
