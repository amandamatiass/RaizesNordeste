package br.com.raizesdonordeste.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import br.com.raizesdonordeste.domain.Canal;
import br.com.raizesdonordeste.domain.HistoricoStatus;
import br.com.raizesdonordeste.domain.ItemPedido;
import br.com.raizesdonordeste.domain.Pedido;
import br.com.raizesdonordeste.domain.StatusPedido;

public final class PedidoDtos {

    private PedidoDtos() {
    }

    public record ItemPedidoRequest(@NotNull Long produtoId, @Positive int quantidade) {
    }

    /**
     * pagamentoAntecipado: se omitido, APP e WEB exigem pagamento antecipado; TOTEM e BALCAO não.
     * clienteId: usado apenas quando um funcionário registra o pedido em nome de um cliente (opcional).
     */
    public record CriarPedidoRequest(
            @NotNull Long unidadeId,
            @NotNull Canal canal,
            Boolean pagamentoAntecipado,
            Long clienteId,
            @NotEmpty @Valid List<ItemPedidoRequest> itens) {
    }

    public record AtualizarStatusRequest(@NotNull StatusPedido status) {
    }

    public record CancelamentoRequest(@NotBlank @Size(max = 200) String motivo) {
    }

    public record DescontoRequest(
            @NotNull @DecimalMin("0.01") BigDecimal valor,
            @NotBlank @Size(max = 200) String motivo) {
    }

    public record ItemPedidoResponse(Long produtoId, String produto, int quantidade, BigDecimal precoUnitario,
                                     BigDecimal subtotal) {

        public static ItemPedidoResponse de(ItemPedido i) {
            return new ItemPedidoResponse(i.getProduto().getId(), i.getProduto().getNome(), i.getQuantidade(),
                    i.getPrecoUnitario(), i.subtotal());
        }
    }

    public record PedidoResponse(Long id, StatusPedido status, Canal canal, boolean pagamentoAntecipado,
                                 Long unidadeId, Long clienteId, BigDecimal valorDesconto, BigDecimal valorTotal,
                                 LocalDateTime criadoEm, List<ItemPedidoResponse> itens) {

        public static PedidoResponse de(Pedido p) {
            List<ItemPedidoResponse> itens = p.getItens().stream().map(ItemPedidoResponse::de).toList();
            Long clienteId = p.getCliente() == null ? null : p.getCliente().getId();
            return new PedidoResponse(p.getId(), p.getStatus(), p.getCanal(), p.isPagamentoAntecipado(),
                    p.getUnidade().getId(), clienteId, p.getValorDesconto(), p.getValorTotal(), p.getCriadoEm(),
                    itens);
        }
    }

    public record HistoricoStatusResponse(Long id, StatusPedido statusAnterior, StatusPedido statusNovo,
                                          Long usuarioId, LocalDateTime dataHora) {

        public static HistoricoStatusResponse de(HistoricoStatus h) {
            Long usuarioId = h.getUsuario() == null ? null : h.getUsuario().getId();
            return new HistoricoStatusResponse(h.getId(), h.getStatusAnterior(), h.getStatusNovo(), usuarioId,
                    h.getDataHora());
        }
    }
}
