package br.com.raizesdonordeste.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import br.com.raizesdonordeste.domain.Campanha;
import br.com.raizesdonordeste.domain.MovimentoPontos;

public final class FidelidadeDtos {

    private FidelidadeDtos() {
    }

    public record CampanhaRequest(
            @NotBlank @Size(max = 120) String nome,
            @Size(max = 300) String regra,
            @NotNull @DecimalMin("1.00") BigDecimal multiplicador,
            @NotNull LocalDate inicio,
            @NotNull LocalDate fim,
            Boolean ativa) {
    }

    public record CampanhaResponse(Long id, String nome, String regra, BigDecimal multiplicador, LocalDate inicio,
                                   LocalDate fim, boolean ativa) {

        public static CampanhaResponse de(Campanha c) {
            return new CampanhaResponse(c.getId(), c.getNome(), c.getRegra(), c.getMultiplicador(), c.getInicio(),
                    c.getFim(), c.isAtiva());
        }
    }

    public record MovimentoPontosResponse(Long id, int pontos, String tipo, Long pedidoId, Long campanhaId,
                                          LocalDateTime dataHora) {

        public static MovimentoPontosResponse de(MovimentoPontos m) {
            Long pedidoId = m.getPedido() == null ? null : m.getPedido().getId();
            Long campanhaId = m.getCampanha() == null ? null : m.getCampanha().getId();
            return new MovimentoPontosResponse(m.getId(), m.getPontos(), m.getTipo(), pedidoId, campanhaId,
                    m.getDataHora());
        }
    }

    public record PontosResponse(long saldo, List<MovimentoPontosResponse> extrato) {
    }
}
