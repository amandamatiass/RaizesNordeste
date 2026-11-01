package br.com.raizesdonordeste.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import br.com.raizesdonordeste.security.AuthUser;
import br.com.raizesdonordeste.service.UsuarioService;
import br.com.raizesdonordeste.web.dto.UsuarioDtos.UsuarioPatchRequest;
import br.com.raizesdonordeste.web.dto.UsuarioDtos.UsuarioRequest;
import br.com.raizesdonordeste.web.dto.UsuarioDtos.UsuarioResponse;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuários e acessos")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN_MATRIZ')")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return service.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody UsuarioRequest req) {
        return service.criar(user, req);
    }

    @PatchMapping("/{id}")
    public UsuarioResponse atualizar(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                     @Valid @RequestBody UsuarioPatchRequest req) {
        return service.atualizar(user, id, req);
    }
}
