package br.com.raizesdonordeste.web.dto;

import java.math.BigDecimal;

public record VendasPorUnidadeDto(Long unidadeId, String unidade, Long pedidos, BigDecimal total) {
}
