package br.com.raizesdonordeste.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.raizesdonordeste.domain.Cliente;
import br.com.raizesdonordeste.domain.Usuario;
import br.com.raizesdonordeste.exception.CredenciaisInvalidasException;
import br.com.raizesdonordeste.repository.ClienteRepository;
import br.com.raizesdonordeste.repository.UsuarioRepository;
import br.com.raizesdonordeste.security.AuthUser;
import br.com.raizesdonordeste.security.JwtService;
import br.com.raizesdonordeste.web.dto.AuthDtos.LoginRequest;
import br.com.raizesdonordeste.web.dto.AuthDtos.TokenResponse;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
                       PasswordEncoder encoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        String email = req.email().trim();

        Optional<Usuario> usuario = usuarioRepository.findByEmailIgnoreCase(email);
        if (usuario.isPresent()) {
            Usuario u = usuario.get();
            if (!u.isAtivo() || !encoder.matches(req.senha(), u.getSenhaHash())) {
                throw new CredenciaisInvalidasException("Credenciais inválidas");
            }
            return tokens(autenticadoDe(u));
        }

        Cliente cliente = clienteRepository.findByEmailIgnoreCase(email)
                .filter(c -> encoder.matches(req.senha(), c.getSenhaHash()))
                .orElseThrow(() -> new CredenciaisInvalidasException("Credenciais inválidas"));
        return tokens(autenticadoDe(cliente));
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {
        AuthUser anterior;
        try {
            anterior = jwtService.validar(refreshToken, "refresh");
        } catch (RuntimeException e) {
            throw new CredenciaisInvalidasException("Refresh token inválido ou expirado");
        }
        // Recarrega do banco: perfil, unidade e situação podem ter mudado desde a emissão do token.
        if ("CLIENTE".equals(anterior.tipo())) {
            Cliente c = clienteRepository.findById(anterior.id())
                    .orElseThrow(() -> new CredenciaisInvalidasException("Usuário não encontrado"));
            return tokens(autenticadoDe(c));
        }
        Usuario u = usuarioRepository.findById(anterior.id())
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new CredenciaisInvalidasException("Usuário inativo ou não encontrado"));
        return tokens(autenticadoDe(u));
    }

    private AuthUser autenticadoDe(Usuario u) {
        Long unidadeId = u.getUnidade() == null ? null : u.getUnidade().getId();
        return new AuthUser(u.getId(), "USUARIO", u.getPerfil().name(), unidadeId, u.getEmail());
    }

    private AuthUser autenticadoDe(Cliente c) {
        return new AuthUser(c.getId(), "CLIENTE", "CLIENTE", null, c.getEmail());
    }

    private TokenResponse tokens(AuthUser u) {
        return new TokenResponse(jwtService.gerarAccessToken(u), jwtService.gerarRefreshToken(u), "Bearer",
                jwtService.segundosAccess());
    }
}
