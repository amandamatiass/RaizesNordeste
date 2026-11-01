package br.com.raizesdonordeste.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import br.com.raizesdonordeste.domain.Produto;

public final class CardapioDtos {

    private CardapioDtos() {
    }

    public record ProdutoRequest(
            @NotBlank @Size(max = 120) String nome,
            @Size(max = 400) String descricao,
            @NotNull @DecimalMin("0.01") BigDecimal precoBase,
            boolean sazonal) {
    }

    public record ProdutoResponse(Long id, String nome, String descricao, BigDecimal precoBase, boolean sazonal) {

        public static ProdutoResponse de(Produto p) {
            return new ProdutoResponse(p.getId(), p.getNome(), p.getDescricao(), p.getPrecoBase(), p.isSazonal());
        }
    }

    public record ItemCardapioRequest(
            @NotNull @DecimalMin("0.01") BigDecimal preco,
            Boolean habilitado,
            LocalDate inicioVenda,
            LocalDate fimVenda) {
    }

    public record ItemCardapioResponse(Long produtoId, String nome, String descricao, BigDecimal preco,
                                       boolean sazonal, LocalDate inicioVenda, LocalDate fimVenda,
                                       boolean habilitado, boolean disponivelEmEstoque) {
    }
}
