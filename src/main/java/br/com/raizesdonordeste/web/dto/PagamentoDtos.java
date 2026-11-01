package br.com.raizesdonordeste.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.com.raizesdonordeste.domain.Pagamento;
import br.com.raizesdonordeste.domain.StatusPagamento;

public final class PagamentoDtos {

    private PagamentoDtos() {
    }

    public record PagamentoResponse(Long id, Long pedidoId, String provedor, String idTransacaoExterno,
                                    StatusPagamento status, BigDecimal valor, LocalDateTime atualizadoEm) {

        public static PagamentoResponse de(Pagamento p) {
            return new PagamentoResponse(p.getId(), p.getPedido().getId(), p.getProvedor(),
                    p.getIdTransacaoExterno(), p.getStatus(), p.getValor(), p.getAtualizadoEm());
        }
    }

    /** Corpo da notificação enviada pelo provedor de pagamento. */
    public record RetornoPagamento(String transacaoId, String status, BigDecimal valor) {
    }
}
