package br.com.raizesdonordeste.security;

/**
 * Identidade autenticada extraída do token JWT.
 * tipo: USUARIO (equipe interna) ou CLIENTE. perfil: ADMIN_MATRIZ, GERENTE, FUNCIONARIO ou CLIENTE.
 */
public record AuthUser(Long id, String tipo, String perfil, Long unidadeId, String email) {
}
