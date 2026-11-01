package br.com.raizesdonordeste.web.dto;

import java.time.LocalDateTime;

import br.com.raizesdonordeste.domain.RegistroHistorico;

public final class HistoricoDtos {

    private HistoricoDtos() {
    }

    public record HistoricoResponse(Long id, Long usuarioId, String usuario, String acao, String entidade,
                                    Long entidadeId, String dadosAntes, String dadosDepois,
                                    LocalDateTime dataHora) {

        public static HistoricoResponse de(RegistroHistorico r) {
            return new HistoricoResponse(r.getId(), r.getUsuario().getId(), r.getUsuario().getNome(), r.getAcao(),
                    r.getEntidade(), r.getEntidadeId(), r.getDadosAntes(), r.getDadosDepois(), r.getDataHora());
        }
    }
}
