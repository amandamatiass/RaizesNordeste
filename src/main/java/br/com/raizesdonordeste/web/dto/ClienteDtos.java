package br.com.raizesdonordeste.web.dto;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import br.com.raizesdonordeste.domain.Cliente;
import br.com.raizesdonordeste.domain.Consentimento;

public final class ClienteDtos {

    private ClienteDtos() {
    }

    public record ClienteRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Email @Size(max = 160) String email,
            @Size(max = 30) String telefone,
            @NotBlank @Size(min = 6, max = 72) String senha) {
    }

    public record ClienteResponse(Long id, String nome, String email, String telefone) {

        public static ClienteResponse de(Cliente c) {
            return new ClienteResponse(c.getId(), c.getNome(), c.getEmail(), c.getTelefone());
        }
    }

    public record ConsentimentoRequest(
            @NotBlank @Size(max = 60) String finalidade,
            @NotBlank @Size(max = 60) String baseLegal,
            @NotNull Boolean concedido) {
    }

    public record ConsentimentosRequest(@NotEmpty @Valid List<ConsentimentoRequest> consentimentos) {
    }

    public record ConsentimentoResponse(String finalidade, String baseLegal, boolean concedido,
                                        LocalDateTime registradoEm) {

        public static ConsentimentoResponse de(Consentimento c) {
            return new ConsentimentoResponse(c.getFinalidade(), c.getBaseLegal(), c.isConcedido(),
                    c.getRegistradoEm());
        }
    }
}
