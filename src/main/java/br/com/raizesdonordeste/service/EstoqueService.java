package br.com.raizesdonordeste.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import br.com.raizesdonordeste.domain.Estoque;
import br.com.raizesdonordeste.domain.MovimentacaoEstoque;
import br.com.raizesdonordeste.domain.Produto;
import br.com.raizesdonordeste.domain.TipoMovimentacao;
import br.com.raizesdonordeste.domain.Unidade;
import br.com.raizesdonordeste.domain.Usuario;
import br.com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.exception.RegraNegocioException;
import br.com.raizesdonordeste.repository.EstoqueRepository;
import br.com.raizesdonordeste.repository.MovimentacaoEstoqueRepository;
import br.com.raizesdonordeste.repository.ProdutoRepository;
import br.com.raizesdonordeste.repository.UnidadeRepository;
import br.com.raizesdonordeste.repository.UsuarioRepository;
import br.com.raizesdonordeste.security.Acesso;
import br.com.raizesdonordeste.security.AuthUser;
import br.com.raizesdonordeste.web.dto.EstoqueDtos.EstoqueResponse;
import br.com.raizesdonordeste.web.dto.EstoqueDtos.MovimentacaoRequest;
import br.com.raizesdonordeste.web.dto.EstoqueDtos.MovimentacaoResponse;

@Service
public class EstoqueService {

    private final EstoqueRepository estoqueRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final UnidadeRepository unidadeRepository;
    private final ProdutoRepository produtoRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistoricoService historico;
    private final EntityManager em;

    public EstoqueService(EstoqueRepository estoqueRepository, MovimentacaoEstoqueRepository movimentacaoRepository,
                          UnidadeRepository unidadeRepository, ProdutoRepository produtoRepository,
                          UsuarioRepository usuarioRepository, HistoricoService historico, EntityManager em) {
        this.estoqueRepository = estoqueRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.unidadeRepository = unidadeRepository;
        this.produtoRepository = produtoRepository;
        this.usuarioRepository = usuarioRepository;
        this.historico = historico;
        this.em = em;
    }

    @Transactional(readOnly = true)
    public List<EstoqueResponse> posicao(AuthUser user, Long unidadeId) {
        Acesso.exigirUnidade(user, unidadeId);
        return estoqueRepository.findByUnidadeIdOrderByProdutoNome(unidadeId).stream()
                .map(EstoqueResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoResponse> movimentacoes(AuthUser user, Long unidadeId, int page, int size) {
        Acesso.exigirUnidade(user, unidadeId);
        PageRequest pagina = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return movimentacaoRepository.findByEstoqueUnidadeIdOrderByDataHoraDesc(unidadeId, pagina).stream()
                .map(MovimentacaoResponse::de).toList();
    }

    /** Entrada, saída ou ajuste manual de estoque. Nunca deixa o saldo negativo nem abaixo do reservado. */
    @Transactional
    public MovimentacaoResponse movimentar(AuthUser user, Long unidadeId, MovimentacaoRequest req) {
        Acesso.exigirUnidade(user, unidadeId);
        if (req.tipo() == TipoMovimentacao.RESERVA || req.tipo() == TipoMovimentacao.LIBERACAO) {
            throw new RegraNegocioException("Reservas e liberações são geradas automaticamente pelos pedidos");
        }
        if (req.tipo() != TipoMovimentacao.AJUSTE && req.quantidade() <= 0) {
            throw new RegraNegocioException("Informe uma quantidade maior que zero");
        }
        Unidade unidade = unidadeRepository.findById(unidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade não encontrada: " + unidadeId));
        Produto produto = produtoRepository.findById(req.produtoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado: " + req.produtoId()));
        Estoque estoque = estoqueRepository.findByUnidadeIdAndProdutoId(unidadeId, produto.getId())
                .orElseGet(() -> criarEstoque(unidade, produto));

        int quantidadeAntes = estoque.getQuantidade();
        int reservadoAntes = estoque.getReservado();
        int quantidadeMovimento = req.quantidade();
        int linhas;
        switch (req.tipo()) {
            case ENTRADA:
                linhas = estoqueRepository.entrada(estoque.getId(), req.quantidade());
                break;
            case SAIDA:
                linhas = estoqueRepository.saida(estoque.getId(), req.quantidade());
                break;
            default:
                linhas = estoqueRepository.ajustar(estoque.getId(), req.quantidade());
                quantidadeMovimento = req.quantidade() - quantidadeAntes;
                break;
        }
        if (linhas == 0) {
            throw new RegraNegocioException(
                    "Operação recusada: o estoque ficaria negativo ou abaixo da quantidade reservada por pedidos");
        }
        em.refresh(estoque);

        Usuario operador = usuarioRepository.findById(user.id()).orElse(null);
        MovimentacaoEstoque mov = new MovimentacaoEstoque();
        mov.setEstoque(estoque);
        mov.setUsuario(operador);
        mov.setTipo(req.tipo());
        mov.setQuantidade(quantidadeMovimento);
        mov.setMotivo(req.motivo());
        mov.setDataHora(LocalDateTime.now());
        MovimentacaoEstoque salva = movimentacaoRepository.save(mov);

        historico.registrar(user, "MOVIMENTACAO_ESTOQUE", "estoque", estoque.getId(),
                HistoricoService.dados("produtoId", produto.getId(), "quantidade", quantidadeAntes,
                        "reservado", reservadoAntes),
                HistoricoService.dados("tipo", req.tipo().name(), "quantidade", estoque.getQuantidade(),
                        "reservado", estoque.getReservado(), "motivo", req.motivo()));
        return MovimentacaoResponse.de(salva);
    }

    private Estoque criarEstoque(Unidade unidade, Produto produto) {
        Estoque novo = new Estoque();
        novo.setUnidade(unidade);
        novo.setProduto(produto);
        novo.setQuantidade(0);
        novo.setReservado(0);
        return estoqueRepository.save(novo);
    }
}
