package br.com.raizesdonordeste.integracao;

import java.math.BigDecimal;

/**
 * Contrato com o provedor de pagamento externo. O sistema apenas encaminha a solicitação e registra o retorno:
 * a transação financeira é processada pelo provedor.
 */
public interface ProvedorPagamento {

    String nome();

    SolicitacaoPagamento solicitarPagamento(Long pedidoId, BigDecimal valor);

    record SolicitacaoPagamento(String idTransacaoExterno) {
    }
}
