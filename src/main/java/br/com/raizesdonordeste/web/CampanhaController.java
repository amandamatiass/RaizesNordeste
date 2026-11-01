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

import br.com.raizesdonordeste.service.FidelidadeService;
import br.com.raizesdonordeste.web.dto.FidelidadeDtos.CampanhaRequest;
import br.com.raizesdonordeste.web.dto.FidelidadeDtos.CampanhaResponse;

@RestController
@RequestMapping("/api/v1/campanhas")
@Tag(name = "Campanhas de fidelidade")
@SecurityRequirement(name = "bearerAuth")
public class CampanhaController {

    private final FidelidadeService service;

    public CampanhaController(FidelidadeService service) {
        this.service = service;
    }

    @GetMapping
    public List<CampanhaResponse> listar() {
        return service.listarCampanhas();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN_MATRIZ')")
    public CampanhaResponse criar(@Valid @RequestBody CampanhaRequest req) {
        return service.criarCampanha(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_MATRIZ')")
    public CampanhaResponse atualizar(@PathVariable Long id, @Valid @RequestBody CampanhaRequest req) {
        return service.atualizarCampanha(id, req);
    }
}
