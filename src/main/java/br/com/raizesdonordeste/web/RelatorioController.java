package br.com.raizesdonordeste.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import br.com.raizesdonordeste.service.RelatorioService;
import br.com.raizesdonordeste.web.dto.ProdutoRankingDto;
import br.com.raizesdonordeste.web.dto.VendasPorUnidadeDto;

@RestController
@RequestMapping("/api/v1/relatorios")
@Tag(name = "Relatórios gerenciais")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN_MATRIZ')")
public class RelatorioController {

    private final RelatorioService service;

    public RelatorioController(RelatorioService service) {
        this.service = service;
    }

    @GetMapping("/vendas")
    public List<VendasPorUnidadeDto> vendas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) Long unidadeId) {
        return service.vendas(de, ate, unidadeId);
    }

    @GetMapping("/produtos-mais-vendidos")
    public List<ProdutoRankingDto> produtosMaisVendidos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(defaultValue = "10") int limite) {
        return service.produtosMaisVendidos(de, ate, limite);
    }
}
