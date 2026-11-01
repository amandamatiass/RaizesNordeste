package br.com.raizesdonordeste.integracao;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * Implementação de desenvolvimento: gera um identificador de transação e não chama nenhum serviço externo.
 * O "retorno" do provedor é simulado chamando POST /api/v1/webhooks/pagamentos.
 * Para um provedor real, crie outra implementação de {@link ProvedorPagamento}.
 */
@Component
public class ProvedorPagamentoSimulado implements ProvedorPagamento {

    @Override
    public String nome() {
        return "SIMULADO";
    }

    @Override
    public SolicitacaoPagamento solicitarPagamento(Long pedidoId, BigDecimal valor) {
        String id = "tx_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return new SolicitacaoPagamento(id);
    }
}
