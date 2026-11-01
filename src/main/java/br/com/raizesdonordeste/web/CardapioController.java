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

import br.com.raizesdonordeste.service.CardapioService;
import br.com.raizesdonordeste.web.dto.CardapioDtos.ItemCardapioRequest;
import br.com.raizesdonordeste.web.dto.CardapioDtos.ItemCardapioResponse;
import br.com.raizesdonordeste.web.dto.CardapioDtos.ProdutoRequest;
import br.com.raizesdonordeste.web.dto.CardapioDtos.ProdutoResponse;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Produtos e cardápio")
public class CardapioController {

    private final CardapioService service;

    public CardapioController(CardapioService service) {
        this.service = service;
    }

    @GetMapping("/produtos")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN_MATRIZ','GERENTE')")
    public List<ProdutoResponse> listarProdutos() {
        return service.listarProdutos();
    }

    @PostMapping("/produtos")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN_MATRIZ')")
    public ProdutoResponse criarProduto(@Valid @RequestBody ProdutoRequest req) {
        return service.criarProduto(req);
    }

    /** Público: itens habilitados e dentro do período de venda, com o preço da unidade. */
    @GetMapping("/unidades/{unidadeId}/cardapio")
    public List<ItemCardapioResponse> cardapio(@PathVariable Long unidadeId) {
        return service.cardapio(unidadeId);
    }

    @PutMapping("/unidades/{unidadeId}/cardapio/{produtoId}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN_MATRIZ')")
    public ItemCardapioResponse configurarItem(@PathVariable Long unidadeId, @PathVariable Long produtoId,
                                               @Valid @RequestBody ItemCardapioRequest req) {
        return service.configurarItem(unidadeId, produtoId, req);
    }
}
