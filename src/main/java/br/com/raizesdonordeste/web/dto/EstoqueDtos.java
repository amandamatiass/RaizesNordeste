package br.com.raizesdonordeste.web.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import br.com.raizesdonordeste.domain.Estoque;
import br.com.raizesdonordeste.domain.MovimentacaoEstoque;
import br.com.raizesdonordeste.domain.TipoMovimentacao;

public final class EstoqueDtos {

    private EstoqueDtos() {
    }

    public record EstoqueResponse(Long produtoId, String produto, int quantidade, int reservado, int disponivel) {

        public static EstoqueResponse de(Estoque e) {
            return new EstoqueResponse(e.getProduto().getId(), e.getProduto().getNome(), e.getQuantidade(),
                    e.getReservado(), e.disponivel());
        }
    }

    /**
     * ENTRADA soma, SAIDA subtrai e AJUSTE define a nova quantidade total (contagem física).
     * RESERVA e LIBERACAO são geradas automaticamente pelos pedidos.
     */
    public record MovimentacaoRequest(
            @NotNull Long produtoId,
            @NotNull TipoMovimentacao tipo,
            @PositiveOrZero int quantidade,
            @NotBlank @Size(max = 200) String motivo) {
    }

    public record MovimentacaoResponse(Long id, Long produtoId, String produto, TipoMovimentacao tipo,
                                       int quantidade, Long pedidoId, Long usuarioId, String motivo,
                                       LocalDateTime dataHora) {

        public static MovimentacaoResponse de(MovimentacaoEstoque m) {
            Long pedidoId = m.getPedido() == null ? null : m.getPedido().getId();
            Long usuarioId = m.getUsuario() == null ? null : m.getUsuario().getId();
            return new MovimentacaoResponse(m.getId(), m.getEstoque().getProduto().getId(),
                    m.getEstoque().getProduto().getNome(), m.getTipo(), m.getQuantidade(), pedidoId, usuarioId,
                    m.getMotivo(), m.getDataHora());
        }
    }
}
