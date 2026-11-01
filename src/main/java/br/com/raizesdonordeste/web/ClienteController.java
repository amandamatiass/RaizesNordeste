package br.com.raizesdonordeste.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import br.com.raizesdonordeste.security.AuthUser;
import br.com.raizesdonordeste.service.ClienteService;
import br.com.raizesdonordeste.service.FidelidadeService;
import br.com.raizesdonordeste.web.dto.ClienteDtos.ClienteRequest;
import br.com.raizesdonordeste.web.dto.ClienteDtos.ClienteResponse;
import br.com.raizesdonordeste.web.dto.ClienteDtos.ConsentimentosRequest;
import br.com.raizesdonordeste.web.dto.ClienteDtos.ConsentimentoResponse;
import br.com.raizesdonordeste.web.dto.FidelidadeDtos.PontosResponse;

@RestController
@RequestMapping("/api/v1/clientes")
@Tag(name = "Clientes, LGPD e fidelidade")
public class ClienteController {

    private final ClienteService clienteService;
    private final FidelidadeService fidelidadeService;

    public ClienteController(ClienteService clienteService, FidelidadeService fidelidadeService) {
        this.clienteService = clienteService;
        this.fidelidadeService = fidelidadeService;
    }

    /** Público: autocadastro de cliente. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse cadastrar(@Valid @RequestBody ClienteRequest req) {
        return clienteService.cadastrar(req);
    }

    @GetMapping("/me/consentimentos")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('CLIENTE')")
    public List<ConsentimentoResponse> consentimentos(@AuthenticationPrincipal AuthUser user) {
        return clienteService.consentimentos(user.id());
    }

    @PutMapping("/me/consentimentos")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('CLIENTE')")
    public List<ConsentimentoResponse> atualizarConsentimentos(
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody ConsentimentosRequest req) {
        return clienteService.atualizarConsentimentos(user.id(), req.consentimentos());
    }

    @GetMapping("/me/pontos")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('CLIENTE')")
    public PontosResponse pontos(@AuthenticationPrincipal AuthUser user) {
        return fidelidadeService.pontos(user.id());
    }
}
