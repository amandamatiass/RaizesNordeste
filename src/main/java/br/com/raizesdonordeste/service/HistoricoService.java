package br.com.raizesdonordeste.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.raizesdonordeste.domain.RegistroHistorico;
import br.com.raizesdonordeste.domain.Usuario;
import br.com.raizesdonordeste.repository.RegistroHistoricoRepository;
import br.com.raizesdonordeste.repository.UsuarioRepository;
import br.com.raizesdonordeste.security.AuthUser;
import br.com.raizesdonordeste.web.dto.HistoricoDtos.HistoricoResponse;

/**
 * Histórico de operações críticas (cancelamentos, descontos, ajustes de estoque, alterações de acesso).
 * O registro é gravado na mesma transação da operação: se a operação falhar, o registro também não existe.
 */
@Service
public class HistoricoService {

    private final RegistroHistoricoRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper mapper;

    public HistoricoService(RegistroHistoricoRepository repository, UsuarioRepository usuarioRepository,
                            ObjectMapper mapper) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.mapper = mapper;
    }

    /** Monta um mapa ordenado a partir de pares chave/valor: dados("status", "X", "motivo", "Y"). */
    public static Map<String, Object> dados(Object... paresChaveValor) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i + 1 < paresChaveValor.length; i += 2) {
            mapa.put(String.valueOf(paresChaveValor[i]), paresChaveValor[i + 1]);
        }
        return mapa;
    }

    @Transactional
    public void registrar(AuthUser ator, String acao, String entidade, Long entidadeId,
                          Map<String, Object> antes, Map<String, Object> depois) {
        Usuario usuario = usuarioRepository.findById(ator.id())
                .orElseThrow(() -> new IllegalStateException("Usuário responsável pelo registro não encontrado"));
        RegistroHistorico registro = new RegistroHistorico();
        registro.setUsuario(usuario);
        registro.setAcao(acao);
        registro.setEntidade(entidade);
        registro.setEntidadeId(entidadeId);
        registro.setDadosAntes(paraJson(antes));
        registro.setDadosDepois(paraJson(depois));
        registro.setDataHora(LocalDateTime.now());
        repository.save(registro);
    }

    @Transactional(readOnly = true)
    public List<HistoricoResponse> consultar(String acao, Long usuarioId, LocalDate de, LocalDate ate,
                                             int page, int size) {
        Specification<RegistroHistorico> spec = (root, query, cb) -> cb.conjunction();
        if (acao != null && !acao.isBlank()) {
            spec = spec.and(porAcao(acao.trim()));
        }
        if (usuarioId != null) {
            spec = spec.and(porUsuario(usuarioId));
        }
        if (de != null) {
            spec = spec.and(desde(de.atStartOfDay()));
        }
        if (ate != null) {
            spec = spec.and(ate(ate.plusDays(1).atStartOfDay()));
        }
        PageRequest pagina = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "dataHora"));
        return repository.findAll(spec, pagina).getContent().stream().map(HistoricoResponse::de).toList();
    }

    private static Specification<RegistroHistorico> porAcao(String acao) {
        return (root, query, cb) -> cb.equal(root.get("acao"), acao);
    }

    private static Specification<RegistroHistorico> porUsuario(Long usuarioId) {
        return (root, query, cb) -> cb.equal(root.get("usuario").get("id"), usuarioId);
    }

    private static Specification<RegistroHistorico> desde(LocalDateTime inicio) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<LocalDateTime>get("dataHora"), inicio);
    }

    private static Specification<RegistroHistorico> ate(LocalDateTime fimExclusivo) {
        return (root, query, cb) -> cb.lessThan(root.<LocalDateTime>get("dataHora"), fimExclusivo);
    }

    private String paraJson(Map<String, Object> dados) {
        if (dados == null) {
            return null;
        }
        try {
            return mapper.writeValueAsString(dados);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível serializar o histórico", e);
        }
    }
}
