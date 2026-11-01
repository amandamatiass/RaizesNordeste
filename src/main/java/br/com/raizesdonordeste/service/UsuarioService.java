package br.com.raizesdonordeste.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.raizesdonordeste.domain.Perfil;
import br.com.raizesdonordeste.domain.Unidade;
import br.com.raizesdonordeste.domain.Usuario;
import br.com.raizesdonordeste.exception.ConflitoException;
import br.com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.exception.RegraNegocioException;
import br.com.raizesdonordeste.repository.UnidadeRepository;
import br.com.raizesdonordeste.repository.UsuarioRepository;
import br.com.raizesdonordeste.security.AuthUser;
import br.com.raizesdonordeste.web.dto.UsuarioDtos.UsuarioPatchRequest;
import br.com.raizesdonordeste.web.dto.UsuarioDtos.UsuarioRequest;
import br.com.raizesdonordeste.web.dto.UsuarioDtos.UsuarioResponse;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UnidadeRepository unidadeRepository;
    private final PasswordEncoder encoder;
    private final HistoricoService historico;

    public UsuarioService(UsuarioRepository usuarioRepository, UnidadeRepository unidadeRepository,
                          PasswordEncoder encoder, HistoricoService historico) {
        this.usuarioRepository = usuarioRepository;
        this.unidadeRepository = unidadeRepository;
        this.encoder = encoder;
        this.historico = historico;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(UsuarioResponse::de).toList();
    }

    @Transactional
    public UsuarioResponse criar(AuthUser ator, UsuarioRequest req) {
        if (usuarioRepository.existsByEmailIgnoreCase(req.email().trim())) {
            throw new ConflitoException("Já existe um usuário com este e-mail");
        }
        Unidade unidade = resolverUnidade(req.perfil(), req.unidadeId());
        Usuario u = new Usuario();
        u.setNome(req.nome().trim());
        u.setEmail(req.email().trim().toLowerCase());
        u.setSenhaHash(encoder.encode(req.senha()));
        u.setPerfil(req.perfil());
        u.setUnidade(unidade);
        u.setAtivo(true);
        Usuario salvo = usuarioRepository.save(u);
        historico.registrar(ator, "CRIACAO_USUARIO", "usuario", salvo.getId(), null,
                HistoricoService.dados("email", salvo.getEmail(), "perfil", salvo.getPerfil().name(),
                        "unidadeId", unidade == null ? null : unidade.getId()));
        return UsuarioResponse.de(salvo);
    }

    @Transactional
    public UsuarioResponse atualizar(AuthUser ator, Long id, UsuarioPatchRequest req) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + id));
        Long unidadeAntes = u.getUnidade() == null ? null : u.getUnidade().getId();
        Perfil perfilAntes = u.getPerfil();
        boolean ativoAntes = u.isAtivo();

        Perfil perfilNovo = req.perfil() != null ? req.perfil() : u.getPerfil();
        Long unidadeIdNova = req.unidadeId() != null ? req.unidadeId() : unidadeAntes;
        Unidade unidade = resolverUnidade(perfilNovo, unidadeIdNova);

        if (req.nome() != null && !req.nome().isBlank()) {
            u.setNome(req.nome().trim());
        }
        if (req.senha() != null) {
            u.setSenhaHash(encoder.encode(req.senha()));
        }
        if (req.ativo() != null) {
            u.setAtivo(req.ativo());
        }
        u.setPerfil(perfilNovo);
        u.setUnidade(unidade);

        historico.registrar(ator, "ALTERACAO_USUARIO", "usuario", u.getId(),
                HistoricoService.dados("perfil", perfilAntes.name(), "unidadeId", unidadeAntes, "ativo", ativoAntes),
                HistoricoService.dados("perfil", u.getPerfil().name(),
                        "unidadeId", unidade == null ? null : unidade.getId(), "ativo", u.isAtivo(),
                        "senhaAlterada", req.senha() != null));
        return UsuarioResponse.de(u);
    }

    /** Gerente e funcionário pertencem a uma unidade; o administrador da matriz não. */
    private Unidade resolverUnidade(Perfil perfil, Long unidadeId) {
        if (perfil == Perfil.ADMIN_MATRIZ) {
            return null;
        }
        if (unidadeId == null) {
            throw new RegraNegocioException("Gerente e funcionário precisam estar vinculados a uma unidade");
        }
        return unidadeRepository.findById(unidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade não encontrada: " + unidadeId));
    }
}
