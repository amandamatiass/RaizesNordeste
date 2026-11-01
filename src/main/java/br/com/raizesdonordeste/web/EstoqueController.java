package br.com.raizesdonordeste.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import br.com.raizesdonordeste.security.AuthUser;
import br.com.raizesdonordeste.service.EstoqueService;
import br.com.raizesdonordeste.web.dto.EstoqueDtos.EstoqueResponse;
import br.com.raizesdonordeste.web.dto.EstoqueDtos.MovimentacaoRequest;
import br.com.raizesdonordeste.web.dto.EstoqueDtos.MovimentacaoResponse;

@RestController
@RequestMapping("/api/v1/unidades/{unidadeId}/estoque")
@Tag(name = "Estoque")
@SecurityRequirement(name = "bearerAuth")
public class EstoqueController {

    private final EstoqueService service;

    public EstoqueController(EstoqueService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_MATRIZ','GERENTE','FUNCIONARIO')")
    public List<EstoqueResponse> posicao(@AuthenticationPrincipal AuthUser user, @PathVariable Long unidadeId) {
        return service.posicao(user, unidadeId);
    }

    @PostMapping("/movimentacoes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN_MATRIZ','GERENTE')")
    public MovimentacaoResponse movimentar(@AuthenticationPrincipal AuthUser user, @PathVariable Long unidadeId,
                                           @Valid @RequestBody MovimentacaoRequest req) {
        return service.movimentar(user, unidadeId, req);
    }

    @GetMapping("/movimentacoes")
    @PreAuthorize("hasAnyRole('ADMIN_MATRIZ','GERENTE')")
    public List<MovimentacaoResponse> movimentacoes(@AuthenticationPrincipal AuthUser user,
                                                    @PathVariable Long unidadeId,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return service.movimentacoes(user, unidadeId, page, size);
    }
}
