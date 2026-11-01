package br.com.raizesdonordeste.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.raizesdonordeste.domain.Unidade;
import br.com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.repository.UnidadeRepository;
import br.com.raizesdonordeste.web.dto.UnidadeDtos.UnidadeRequest;
import br.com.raizesdonordeste.web.dto.UnidadeDtos.UnidadeResponse;

@Service
public class UnidadeService {

    private final UnidadeRepository repository;

    public UnidadeService(UnidadeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<UnidadeResponse> listar() {
        return repository.findByAtivaTrueOrderByNome().stream().map(UnidadeResponse::de).toList();
    }

    @Transactional
    public UnidadeResponse criar(UnidadeRequest req) {
        Unidade u = new Unidade();
        aplicar(u, req);
        return UnidadeResponse.de(repository.save(u));
    }

    @Transactional
    public UnidadeResponse atualizar(Long id, UnidadeRequest req) {
        Unidade u = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade não encontrada: " + id));
        aplicar(u, req);
        return UnidadeResponse.de(u);
    }

    private void aplicar(Unidade u, UnidadeRequest req) {
        u.setNome(req.nome().trim());
        u.setCidade(req.cidade());
        u.setRegiao(req.regiao());
        u.setTipoAtendimento(req.tipoAtendimento());
        u.setHorarioFuncionamento(req.horarioFuncionamento());
        u.setAtiva(req.ativa() == null || req.ativa());
    }
}
