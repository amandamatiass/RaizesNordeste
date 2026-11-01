package br.com.raizesdonordeste;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Percorre o fluxo principal usando os dados de exemplo criados pelo DataLoader (banco H2 em memória). */
@SpringBootTest
@AutoConfigureMockMvc
class FluxoPedidoIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Value("${app.webhook.secret}")
    private String segredoWebhook;

    // ------------------------------------------------------------------ utilitários

    private String login(String email, String senha) throws Exception {
        String corpo = json.writeValueAsString(Map.of("email", email, "senha", senha));
        String resposta = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(resposta).get("accessToken").asText();
    }

    private JsonNode lerJson(String url, String token) throws Exception {
        String resposta = mvc.perform(get(url).header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(resposta);
    }

    private long primeiraUnidade() throws Exception {
        String resposta = mvc.perform(get("/api/v1/unidades"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(resposta).get(0).get("id").asLong();
    }

    private long primeiroProdutoDisponivel(long unidadeId) throws Exception {
        String resposta = mvc.perform(get("/api/v1/unidades/" + unidadeId + "/cardapio"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(resposta).get(0).get("produtoId").asLong();
    }

    private String corpoPedido(long unidadeId, long produtoId, int quantidade, boolean antecipado) throws Exception {
        return json.writeValueAsString(Map.of(
                "unidadeId", unidadeId,
                "canal", antecipado ? "APP" : "BALCAO",
                "pagamentoAntecipado", antecipado,
                "itens", new Object[] {Map.of("produtoId", produtoId, "quantidade", quantidade)}));
    }

    private String assinar(String corpo) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(segredoWebhook.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(corpo.getBytes(StandardCharsets.UTF_8)));
    }

    private void patchStatus(long pedidoId, String token, String novo, int esperado) throws Exception {
        mvc.perform(patch("/api/v1/pedidos/" + pedidoId + "/status")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("status", novo))))
                .andExpect(status().is(esperado));
    }

    // ------------------------------------------------------------------ testes

    @Test
    void pedidoComPagamentoAntecipadoPercorreOFluxoCompleto() throws Exception {
        String cliente = login("cliente@teste.com", "123456");
        String funcionario = login("func.recife@raizes.com", "123456");
        long unidadeId = primeiraUnidade();
        long produtoId = primeiroProdutoDisponivel(unidadeId);

        // 1. Cria o pedido: nasce aguardando pagamento.
        String corpo = corpoPedido(unidadeId, produtoId, 2, true);
        String resposta = mvc.perform(post("/api/v1/pedidos").header("Authorization", cliente)
                        .header("Idempotency-Key", "chave-fluxo-completo")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode pedido = json.readTree(resposta);
        long pedidoId = pedido.get("id").asLong();
        assertEquals("AGUARDANDO_PAGAMENTO", pedido.get("status").asText());

        // 2. Repetir a mesma chave de idempotência devolve o mesmo pedido, sem duplicar.
        String repetida = mvc.perform(post("/api/v1/pedidos").header("Authorization", cliente)
                        .header("Idempotency-Key", "chave-fluxo-completo")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertEquals(pedidoId, json.readTree(repetida).get("id").asLong());

        // 3. Sem pagamento aprovado, o pedido não pode ir para preparação.
        patchStatus(pedidoId, funcionario, "EM_PREPARO", 422);

        // 4. Inicia o pagamento e simula o retorno aprovado do provedor.
        String pagamento = mvc.perform(post("/api/v1/pedidos/" + pedidoId + "/pagamentos")
                        .header("Authorization", cliente))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String transacaoId = json.readTree(pagamento).get("idTransacaoExterno").asText();

        String notificacao = json.writeValueAsString(Map.of("transacaoId", transacaoId, "status", "APROVADO"));
        for (int i = 0; i < 2; i++) { // a segunda notificação é repetida e deve ser ignorada
            mvc.perform(post("/api/v1/webhooks/pagamentos")
                            .header("X-Signature", assinar(notificacao))
                            .contentType(MediaType.APPLICATION_JSON).content(notificacao))
                    .andExpect(status().isOk());
        }
        assertEquals("RECEBIDO", lerJson("/api/v1/pedidos/" + pedidoId, cliente).get("status").asText());

        // 5. A equipe conduz o pedido até a conclusão; só pode seguir a sequência válida.
        patchStatus(pedidoId, funcionario, "PRONTO", 409);
        patchStatus(pedidoId, funcionario, "EM_PREPARO", 200);
        patchStatus(pedidoId, funcionario, "PRONTO", 200);
        patchStatus(pedidoId, funcionario, "CONCLUIDO", 200);

        // 6. Cliente com consentimento acumula pontos.
        JsonNode pontos = lerJson("/api/v1/clientes/me/pontos", cliente);
        assertTrue(pontos.get("saldo").asLong() > 0, "o cliente deveria ter acumulado pontos");

        // 7. O histórico de status registra as etapas percorridas.
        JsonNode historico = lerJson("/api/v1/pedidos/" + pedidoId + "/historico-status", cliente);
        assertTrue(historico.size() >= 5, "esperadas ao menos 5 etapas registradas");
    }

    @Test
    void webhookComAssinaturaInvalidaEhRejeitado() throws Exception {
        String notificacao = json.writeValueAsString(Map.of("transacaoId", "tx_qualquer", "status", "APROVADO"));
        mvc.perform(post("/api/v1/webhooks/pagamentos")
                        .header("X-Signature", "assinatura-errada")
                        .contentType(MediaType.APPLICATION_JSON).content(notificacao))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cancelamentoPelaEquipeEhRegistradoNoHistoricoDeOperacoes() throws Exception {
        String cliente = login("cliente@teste.com", "123456");
        String funcionario = login("func.recife@raizes.com", "123456");
        String admin = login("admin@raizes.com", "admin123");
        long unidadeId = primeiraUnidade();
        long produtoId = primeiroProdutoDisponivel(unidadeId);

        String resposta = mvc.perform(post("/api/v1/pedidos").header("Authorization", cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoPedido(unidadeId, produtoId, 1, false)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long pedidoId = json.readTree(resposta).get("id").asLong();

        // O cliente não pode cancelar (somente a equipe).
        mvc.perform(post("/api/v1/pedidos/" + pedidoId + "/cancelamento").header("Authorization", cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("motivo", "teste"))))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/pedidos/" + pedidoId + "/cancelamento").header("Authorization", funcionario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("motivo", "Cliente desistiu"))))
                .andExpect(status().isOk());

        JsonNode registros = lerJson("/api/v1/historico-operacoes?acao=CANCELAMENTO_PEDIDO", admin);
        boolean encontrou = false;
        for (JsonNode r : registros) {
            if (r.get("entidadeId").asLong() == pedidoId) {
                encontrou = true;
            }
        }
        assertTrue(encontrou, "o cancelamento deveria constar no histórico de operações");
    }

    @Test
    void pedidoAcimaDoEstoqueEhRecusado() throws Exception {
        String cliente = login("cliente@teste.com", "123456");
        long unidadeId = primeiraUnidade();
        long produtoId = primeiroProdutoDisponivel(unidadeId);

        mvc.perform(post("/api/v1/pedidos").header("Authorization", cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoPedido(unidadeId, produtoId, 100000, false)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void endpointsProtegidosExigemAutenticacaoEPerfil() throws Exception {
        mvc.perform(get("/api/v1/usuarios")).andExpect(status().isUnauthorized());
        String cliente = login("cliente@teste.com", "123456");
        mvc.perform(get("/api/v1/usuarios").header("Authorization", cliente)).andExpect(status().isForbidden());
    }
}
