package br.com.raizesdonordeste.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de vida do pedido.
 * RECEBIDO -> EM_PREPARO -> PRONTO -> CONCLUIDO
 * AGUARDANDO_PAGAMENTO -> RECEBIDO (somente com pagamento aprovado)
 * Cancelamento permitido até EM_PREPARO.
 */
public enum StatusPedido {
    RECEBIDO, AGUARDANDO_PAGAMENTO, EM_PREPARO, PRONTO, CONCLUIDO, CANCELADO;

    public Set<StatusPedido> proximosPermitidos() {
        switch (this) {
            case RECEBIDO:
                return EnumSet.of(EM_PREPARO, CANCELADO);
            case AGUARDANDO_PAGAMENTO:
                return EnumSet.of(RECEBIDO, CANCELADO);
            case EM_PREPARO:
                return EnumSet.of(PRONTO, CANCELADO);
            case PRONTO:
                return EnumSet.of(CONCLUIDO);
            default:
                return EnumSet.noneOf(StatusPedido.class);
        }
    }

    public boolean podeIrPara(StatusPedido destino) {
        return proximosPermitidos().contains(destino);
    }

    public boolean isFinal() {
        return this == CONCLUIDO || this == CANCELADO;
    }
}
