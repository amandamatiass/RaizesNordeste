package br.com.raizesdonordeste.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import br.com.raizesdonordeste.domain.StatusPedido;
import br.com.raizesdonordeste.security.AuthUser;
import br.com.raizesdonordeste.service.PedidoService;
import br.com.raizesdonordeste.web.dto.PedidoDtos.AtualizarStatusRequest;
import br.com.raizesdonordeste.web.dto.PedidoDtos.CancelamentoRequest;
import br.com.raizesdonordeste.web.dto.PedidoDtos.CriarPedidoRequest;
import br.com.raizesdonordeste.web.dto.PedidoDtos.DescontoRequest;
import br.com.raizesdonordeste.web.dto.PedidoDtos.HistoricoStatusResponse;
import br.com.raizesdonordeste.web.dto.PedidoDtos.PedidoResponse;

@RestController
@RequestMapping("/api/v1/pedidos")
@Tag(name = "Pedidos")
@SecurityRequirement(name = "bearerAuth")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    /** Cria o pedido (201). Reenviar o mesmo Idempotency-Key devolve o pedido já criado, sem duplicá-lo. */
    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENTE','FUNCIONARIO','GERENTE')")
    public ResponseEntity<PedidoResponse> criar(
            @AuthenticationPrincipal AuthUser user,
            @RequestHeader(value = "Idempotency-Key", required = false) String chave,
            @Valid @RequestBody CriarPedidoRequest req) {
        PedidoResponse resposta = service.criar(user, chave, req);
        return ResponseEntity.created(URI.create("/api/v1/pedidos/" + resposta.id())).body(resposta);
    }

    @GetMapping("/{id}")
    public PedidoResponse buscar(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return service.buscar(user, id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_MATRIZ','GERENTE','FUNCIONARIO')")
    public List<PedidoResponse> listar(@AuthenticationPrincipal AuthUser user,
                                       @RequestParam(required = false) Long unidadeId,
                                       @RequestParam(required = false) StatusPedido status,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return service.listar(user, unidadeId, status, page, size);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('GERENTE','FUNCIONARIO')")
    public PedidoResponse atualizarStatus(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                          @Valid @RequestBody AtualizarStatusRequest req) {
        return service.atualizarStatus(user, id, req.status());
    }

    @PostMapping("/{id}/cancelamento")
    @PreAuthorize("hasAnyRole('GERENTE','FUNCIONARIO')")
    public PedidoResponse cancelar(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                   @Valid @RequestBody CancelamentoRequest req) {
        return service.cancelar(user, id, req.motivo());
    }

    @PostMapping("/{id}/descontos")
    @PreAuthorize("hasRole('GERENTE')")
    public PedidoResponse conceder(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                   @Valid @RequestBody DescontoRequest req) {
        return service.conceder(user, id, req.valor(), req.motivo());
    }

    @GetMapping("/{id}/historico-status")
    public List<HistoricoStatusResponse> historicoStatus(@AuthenticationPrincipal AuthUser user,
                                                         @PathVariable Long id) {
        return service.historicoStatus(user, id);
    }
}
