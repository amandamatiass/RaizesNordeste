package br.com.raizesdonordeste.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.raizesdonordeste.domain.Campanha;
import br.com.raizesdonordeste.domain.Cliente;
import br.com.raizesdonordeste.domain.Consentimento;
import br.com.raizesdonordeste.domain.MovimentoPontos;
import br.com.raizesdonordeste.domain.Pedido;
import br.com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.exception.RegraNegocioException;
import br.com.raizesdonordeste.repository.CampanhaRepository;
import br.com.raizesdonordeste.repository.ConsentimentoRepository;
import br.com.raizesdonordeste.repository.MovimentoPontosRepository;
import br.com.raizesdonordeste.web.dto.FidelidadeDtos.CampanhaRequest;
import br.com.raizesdonordeste.web.dto.FidelidadeDtos.CampanhaResponse;
import br.com.raizesdonordeste.web.dto.FidelidadeDtos.MovimentoPontosResponse;
import br.com.raizesdonordeste.web.dto.FidelidadeDtos.PontosResponse;

/** Programa de fidelidade: pontos só são concedidos a clientes com consentimento para a finalidade FIDELIDADE. */
@Service
public class FidelidadeService {

    public static final String FINALIDADE_FIDELIDADE = "FIDELIDADE";

    private final CampanhaRepository campanhaRepository;
    private final MovimentoPontosRepository pontosRepository;
    private final ConsentimentoRepository consentimentoRepository;

    public FidelidadeService(CampanhaRepository campanhaRepository, MovimentoPontosRepository pontosRepository,
                             ConsentimentoRepository consentimentoRepository) {
        this.campanhaRepository = campanhaRepository;
        this.pontosRepository = pontosRepository;
        this.consentimentoRepository = consentimentoRepository;
    }

    @Transactional(readOnly = true)
    public List<CampanhaResponse> listarCampanhas() {
        return campanhaRepository.findAllByOrderByInicioDesc().stream().map(CampanhaResponse::de).toList();
    }

    @Transactional
    public CampanhaResponse criarCampanha(CampanhaRequest req) {
        Campanha c = new Campanha();
        aplicar(c, req);
        return CampanhaResponse.de(campanhaRepository.save(c));
    }

    @Transactional
    public CampanhaResponse atualizarCampanha(Long id, CampanhaRequest req) {
        Campanha c = campanhaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Campanha não encontrada: " + id));
        aplicar(c, req);
        return CampanhaResponse.de(c);
    }

    @Transactional(readOnly = true)
    public PontosResponse pontos(Long clienteId) {
        Long saldo = pontosRepository.saldo(clienteId);
        List<MovimentoPontosResponse> extrato = pontosRepository
                .findByClienteIdOrderByDataHoraDescIdDesc(clienteId, PageRequest.of(0, 50)).stream()
                .map(MovimentoPontosResponse::de).toList();
        return new PontosResponse(saldo == null ? 0L : saldo, extrato);
    }

    /**
     * Chamado quando o pedido é concluído. Pontos = valor pago (parte inteira) x maior multiplicador das
     * campanhas vigentes. Sem consentimento de FIDELIDADE, nenhum ponto é registrado.
     */
    @Transactional
    public void acumularPontos(Pedido pedido) {
        Cliente cliente = pedido.getCliente();
        if (cliente == null || !temConsentimento(cliente.getId(), FINALIDADE_FIDELIDADE)) {
            return;
        }
        int base = pedido.getValorTotal().setScale(0, RoundingMode.DOWN).intValue();
        if (base <= 0) {
            return;
        }
        BigDecimal multiplicador = BigDecimal.ONE;
        Campanha aplicada = null;
        for (Campanha c : campanhaRepository.findVigentes(LocalDate.now())) {
            if (c.getMultiplicador().compareTo(multiplicador) > 0) {
                multiplicador = c.getMultiplicador();
                aplicada = c;
            }
        }
        int pontos = BigDecimal.valueOf(base).multiply(multiplicador).setScale(0, RoundingMode.DOWN).intValue();
        MovimentoPontos mov = new MovimentoPontos();
        mov.setCliente(cliente);
        mov.setPedido(pedido);
        mov.setCampanha(aplicada);
        mov.setPontos(pontos);
        mov.setTipo("ACUMULO");
        mov.setDataHora(LocalDateTime.now());
        pontosRepository.save(mov);
    }

    /** O consentimento vigente é o registro mais recente da finalidade. */
    public boolean temConsentimento(Long clienteId, String finalidade) {
        List<Consentimento> registros = consentimentoRepository.findByClienteIdOrderByRegistradoEmDescIdDesc(clienteId);
        for (Consentimento c : registros) {
            if (c.getFinalidade().equalsIgnoreCase(finalidade)) {
                return c.isConcedido();
            }
        }
        return false;
    }

    private void aplicar(Campanha c, CampanhaRequest req) {
        if (req.fim().isBefore(req.inicio())) {
            throw new RegraNegocioException("O fim da campanha não pode ser anterior ao início");
        }
        c.setNome(req.nome().trim());
        c.setRegra(req.regra());
        c.setMultiplicador(req.multiplicador());
        c.setInicio(req.inicio());
        c.setFim(req.fim());
        c.setAtiva(req.ativa() == null || req.ativa());
    }
}
