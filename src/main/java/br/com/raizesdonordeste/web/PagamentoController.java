package br.com.raizesdonordeste.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import br.com.raizesdonordeste.security.AuthUser;
import br.com.raizesdonordeste.service.PagamentoService;
import br.com.raizesdonordeste.web.dto.PagamentoDtos.PagamentoResponse;

@RestController
@RequestMapping("/api/v1/pedidos/{pedidoId}/pagamentos")
@Tag(name = "Pagamentos")
@SecurityRequirement(name = "bearerAuth")
public class PagamentoController {

    private final PagamentoService service;

    public PagamentoController(PagamentoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CLIENTE','FUNCIONARIO','GERENTE')")
    public PagamentoResponse iniciar(@AuthenticationPrincipal AuthUser user, @PathVariable Long pedidoId) {
        return service.iniciar(user, pedidoId);
    }

    @GetMapping
    public List<PagamentoResponse> listar(@AuthenticationPrincipal AuthUser user, @PathVariable Long pedidoId) {
        return service.listar(user, pedidoId);
    }
}
