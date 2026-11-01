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

import br.com.raizesdonordeste.service.HistoricoService;
import br.com.raizesdonordeste.web.dto.HistoricoDtos.HistoricoResponse;

@RestController
@RequestMapping("/api/v1/historico-operacoes")
@Tag(name = "Histórico de operações")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN_MATRIZ')")
public class HistoricoController {

    private final HistoricoService service;

    public HistoricoController(HistoricoService service) {
        this.service = service;
    }

    /** Ações registradas: CANCELAMENTO_PEDIDO, CONCESSAO_DESCONTO, MOVIMENTACAO_ESTOQUE, CRIACAO_USUARIO, ... */
    @GetMapping
    public List<HistoricoResponse> consultar(
            @RequestParam(required = false) String acao,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.consultar(acao, usuarioId, de, ate, page, size);
    }
}
