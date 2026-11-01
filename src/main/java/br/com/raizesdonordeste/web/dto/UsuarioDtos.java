package br.com.raizesdonordeste.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import br.com.raizesdonordeste.domain.Perfil;
import br.com.raizesdonordeste.domain.Usuario;

public final class UsuarioDtos {

    private UsuarioDtos() {
    }

    public record UsuarioRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(min = 6, max = 72) String senha,
            @NotNull Perfil perfil,
            Long unidadeId) {
    }

    public record UsuarioPatchRequest(
            @Size(max = 120) String nome,
            Perfil perfil,
            Long unidadeId,
            Boolean ativo,
            @Size(min = 6, max = 72) String senha) {
    }

    public record UsuarioResponse(Long id, String nome, String email, Perfil perfil, Long unidadeId, boolean ativo) {

        public static UsuarioResponse de(Usuario u) {
            Long unidadeId = u.getUnidade() == null ? null : u.getUnidade().getId();
            return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(), unidadeId, u.isAtivo());
        }
    }
}
