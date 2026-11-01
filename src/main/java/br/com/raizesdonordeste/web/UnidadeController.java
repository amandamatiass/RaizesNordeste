package br.com.raizesdonordeste.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import br.com.raizesdonordeste.service.UnidadeService;
import br.com.raizesdonordeste.web.dto.UnidadeDtos.UnidadeRequest;
import br.com.raizesdonordeste.web.dto.UnidadeDtos.UnidadeResponse;

@RestController
@RequestMapping("/api/v1/unidades")
@Tag(name = "Unidades")
public class UnidadeController {

    private final UnidadeService service;

    public UnidadeController(UnidadeService service) {
        this.service = service;
    }

    /** Público: lista as unidades ativas. */
    @GetMapping
    public List<UnidadeResponse> listar() {
        return service.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN_MATRIZ')")
    public UnidadeResponse criar(@Valid @RequestBody UnidadeRequest req) {
        return service.criar(req);
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN_MATRIZ')")
    public UnidadeResponse atualizar(@PathVariable Long id, @Valid @RequestBody UnidadeRequest req) {
        return service.atualizar(id, req);
    }
}
