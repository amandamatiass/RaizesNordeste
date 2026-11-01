package br.com.raizesdonordeste.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.raizesdonordeste.domain.HistoricoStatus;
import br.com.raizesdonordeste.domain.Pagamento;
import br.com.raizesdonordeste.domain.Pedido;
import br.com.raizesdonordeste.domain.StatusPagamento;
import br.com.raizesdonordeste.domain.StatusPedido;
import br.com.raizesdonordeste.exception.AssinaturaInvalidaException;
import br.com.raizesdonordeste.exception.ConflitoException;
import br.com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.exception.RegraNegocioException;
import br.com.raizesdonordeste.integracao.ProvedorPagamento;
import br.com.raizesdonordeste.repository.HistoricoStatusRepository;
import br.com.raizesdonordeste.repository.PagamentoRepository;
import br.com.raizesdonordeste.repository.PedidoRepository;
import br.com.raizesdonordeste.security.Acesso;
import br.com.raizesdonordeste.security.AuthUser;
import br.com.raizesdonordeste.web.dto.PagamentoDtos.PagamentoResponse;
import br.com.raizesdonordeste.web.dto.PagamentoDtos.RetornoPagamento;

@Service
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final PedidoRepository pedidoRepository;
    private final HistoricoStatusRepository historicoStatusRepository;
    private final ProvedorPagamento provedor;
    private final ObjectMapper mapper;
    private final String segredoWebhook;
    private final boolean validarAssinatura;

    public PagamentoService(PagamentoRepository pagamentoRepository, PedidoRepository pedidoRepository,
                            HistoricoStatusRepository historicoStatusRepository, ProvedorPagamento provedor,
                            ObjectMapper mapper,
                            @Value("${app.webhook.secret}") String segredoWebhook,
                            @Value("${app.webhook.validar-assinatura:true}") boolean validarAssinatura) {
        this.pagamentoRepository = pagamentoRepository;
        this.pedidoRepository = pedidoRepository;
        this.historicoStatusRepository = historicoStatusRepository;
        this.provedor = provedor;
        this.mapper = mapper;
        this.segredoWebhook = segredoWebhook;
        this.validarAssinatura = validarAssinatura;
    }

    /** Encaminha a solicitação ao provedor e registra o pagamento como PENDENTE. */
    @Transactional
    public PagamentoResponse iniciar(AuthUser user, Long pedidoId) {
        Pedido pedido = carregarPedido(pedidoId);
        Acesso.exigirAcessoPedido(user, pedido);
        if (pedido.getStatus().isFinal()) {
            throw new ConflitoException("Pedido encerrado: não é possível iniciar pagamento");
        }
        if (pagamentoRepository.existsByPedidoIdAndStatusIn(pedidoId,
                List.of(StatusPagamento.PENDENTE, StatusPagamento.APROVADO))) {
            throw new ConflitoException("Já existe pagamento pendente ou aprovado para este pedido");
        }
        if (pedido.getValorTotal().signum() <= 0) {
            throw new RegraNegocioException("Pedido sem valor a pagar");
        }
        ProvedorPagamento.SolicitacaoPagamento solicitacao =
                provedor.solicitarPagamento(pedido.getId(), pedido.getValorTotal());

        Pagamento pagamento = new Pagamento();
        pagamento.setPedido(pedido);
        pagamento.setProvedor(provedor.nome());
        pagamento.setIdTransacaoExterno(solicitacao.idTransacaoExterno());
        pagamento.setStatus(StatusPagamento.PENDENTE);
        pagamento.setValor(pedido.getValorTotal());
        pagamento.setAtualizadoEm(LocalDateTime.now());
        return PagamentoResponse.de(pagamentoRepository.save(pagamento));
    }

    @Transactional(readOnly = true)
    public List<PagamentoResponse> listar(AuthUser user, Long pedidoId) {
        Pedido pedido = carregarPedido(pedidoId);
        Acesso.exigirAcessoPedido(user, pedido);
        return pagamentoRepository.findByPedidoIdOrderByIdDesc(pedidoId).stream()
                .map(PagamentoResponse::de).toList();
    }

    /**
     * Retorno do provedor. Idempotente: notificações repetidas (ou fora de ordem) para um pagamento que já foi
     * decidido são ignoradas e não geram confirmações duplicadas. Aprovação libera o pedido para a fila
     * (AGUARDANDO_PAGAMENTO -> RECEBIDO); nenhum pedido com pagamento antecipado avança sem ela.
     */
    @Transactional
    public String processarRetorno(String assinatura, String corpo) {
        if (validarAssinatura) {
            conferirAssinatura(assinatura, corpo);
        }
        RetornoPagamento retorno = lerRetorno(corpo);
        StatusPagamento novo = lerStatus(retorno.status());
        Pagamento pagamento = pagamentoRepository.findByIdTransacaoExterno(retorno.transacaoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Transação desconhecida: " + retorno.transacaoId()));
        if (retorno.valor() != null && retorno.valor().compareTo(pagamento.getValor()) != 0) {
            throw new RegraNegocioException("Valor da notificação diverge do valor do pagamento");
        }
        if (pagamento.getStatus() != StatusPagamento.PENDENTE || novo == StatusPagamento.PENDENTE) {
            return "IGNORADO";
        }
        pagamento.setStatus(novo);
        pagamento.setAtualizadoEm(LocalDateTime.now());

        if (novo == StatusPagamento.APROVADO) {
            Pedido pedido = pagamento.getPedido();
            if (pedido.getStatus() == StatusPedido.AGUARDANDO_PAGAMENTO) {
                pedido.mudarStatus(StatusPedido.RECEBIDO);
                HistoricoStatus h = new HistoricoStatus();
                h.setPedido(pedido);
                h.setStatusAnterior(StatusPedido.AGUARDANDO_PAGAMENTO);
                h.setStatusNovo(StatusPedido.RECEBIDO);
                h.setDataHora(LocalDateTime.now());
                historicoStatusRepository.save(h);
            }
        }
        return "PROCESSADO";
    }

    // ------------------------------------------------------------------ apoio

    private Pedido carregarPedido(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado: " + id));
    }

    private RetornoPagamento lerRetorno(String corpo) {
        try {
            RetornoPagamento r = mapper.readValue(corpo, RetornoPagamento.class);
            if (r.transacaoId() == null || r.transacaoId().isBlank() || r.status() == null) {
                throw new RegraNegocioException("Notificação sem transacaoId ou status");
            }
            return r;
        } catch (JsonProcessingException e) {
            throw new RegraNegocioException("Corpo da notificação inválido");
        }
    }

    private StatusPagamento lerStatus(String texto) {
        try {
            return StatusPagamento.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RegraNegocioException("Status de pagamento desconhecido: " + texto);
        }
    }

    /** Assinatura esperada: HMAC-SHA256 (hexadecimal) do corpo bruto, no cabeçalho X-Signature. */
    private void conferirAssinatura(String assinatura, String corpo) {
        if (assinatura == null || assinatura.isBlank()) {
            throw new AssinaturaInvalidaException("Assinatura ausente");
        }
        String recebida = assinatura.trim().toLowerCase();
        if (recebida.startsWith("sha256=")) {
            recebida = recebida.substring(7);
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredoWebhook.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String esperada = HexFormat.of().formatHex(mac.doFinal(corpo.getBytes(StandardCharsets.UTF_8)));
            boolean valida = MessageDigest.isEqual(esperada.getBytes(StandardCharsets.UTF_8),
                    recebida.getBytes(StandardCharsets.UTF_8));
            if (!valida) {
                throw new AssinaturaInvalidaException("Assinatura inválida");
            }
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao calcular a assinatura", e);
        }
    }
}
