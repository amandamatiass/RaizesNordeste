package br.com.raizesdonordeste.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StatusPedidoTest {

    @Test
    void fluxoNormalSegueASequenciaValida() {
        assertTrue(StatusPedido.RECEBIDO.podeIrPara(StatusPedido.EM_PREPARO));
        assertTrue(StatusPedido.EM_PREPARO.podeIrPara(StatusPedido.PRONTO));
        assertTrue(StatusPedido.PRONTO.podeIrPara(StatusPedido.CONCLUIDO));
    }

    @Test
    void naoPodePularEtapas() {
        assertFalse(StatusPedido.RECEBIDO.podeIrPara(StatusPedido.PRONTO));
        assertFalse(StatusPedido.EM_PREPARO.podeIrPara(StatusPedido.CONCLUIDO));
    }

    @Test
    void pedidoAguardandoPagamentoSoVaiParaRecebidoOuCancelado() {
        assertTrue(StatusPedido.AGUARDANDO_PAGAMENTO.podeIrPara(StatusPedido.RECEBIDO));
        assertTrue(StatusPedido.AGUARDANDO_PAGAMENTO.podeIrPara(StatusPedido.CANCELADO));
        assertFalse(StatusPedido.AGUARDANDO_PAGAMENTO.podeIrPara(StatusPedido.EM_PREPARO));
    }

    @Test
    void cancelamentoSoAteEmPreparo() {
        assertTrue(StatusPedido.EM_PREPARO.podeIrPara(StatusPedido.CANCELADO));
        assertFalse(StatusPedido.PRONTO.podeIrPara(StatusPedido.CANCELADO));
    }

    @Test
    void estadosFinaisNaoTemProximo() {
        assertTrue(StatusPedido.CONCLUIDO.isFinal());
        assertTrue(StatusPedido.CANCELADO.isFinal());
        assertTrue(StatusPedido.CONCLUIDO.proximosPermitidos().isEmpty());
        assertTrue(StatusPedido.CANCELADO.proximosPermitidos().isEmpty());
    }
}
