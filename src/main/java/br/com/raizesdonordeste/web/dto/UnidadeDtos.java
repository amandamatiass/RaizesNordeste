package br.com.raizesdonordeste.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import br.com.raizesdonordeste.domain.Unidade;

public final class UnidadeDtos {

    private UnidadeDtos() {
    }

    public record UnidadeRequest(
            @NotBlank @Size(max = 120) String nome,
            @Size(max = 80) String cidade,
            @Size(max = 60) String regiao,
            @Size(max = 40) String tipoAtendimento,
            @Size(max = 120) String horarioFuncionamento,
            Boolean ativa) {
    }

    public record UnidadeResponse(Long id, String nome, String cidade, String regiao, String tipoAtendimento,
                                  String horarioFuncionamento, boolean ativa) {

        public static UnidadeResponse de(Unidade u) {
            return new UnidadeResponse(u.getId(), u.getNome(), u.getCidade(), u.getRegiao(),
                    u.getTipoAtendimento(), u.getHorarioFuncionamento(), u.isAtiva());
        }
    }
}
