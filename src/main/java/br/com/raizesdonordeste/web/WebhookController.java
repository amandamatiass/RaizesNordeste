package br.com.raizesdonordeste.web;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

import br.com.raizesdonordeste.service.PagamentoService;

/**
 * Recebe o retorno do provedor de pagamento. Público (sem JWT), mas protegido por assinatura HMAC-SHA256 do
 * corpo no cabeçalho X-Signature. Responde 200 também para notificações repetidas.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhooks")
public class WebhookController {

    private final PagamentoService service;

    public WebhookController(PagamentoService service) {
        this.service = service;
    }

    @PostMapping("/pagamentos")
    public Map<String, String> pagamentos(@RequestHeader(value = "X-Signature", required = false) String assinatura,
                                          @RequestBody String corpo) {
        return Map.of("resultado", service.processarRetorno(assinatura, corpo));
    }
}
