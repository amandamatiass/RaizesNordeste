package br.com.raizesdonordeste.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey chave;
    private final Duration validadeAccess;
    private final Duration validadeRefresh;

    public JwtService(@Value("${app.jwt.secret}") String segredo,
                      @Value("${app.jwt.access-minutes:60}") long accessMinutes,
                      @Value("${app.jwt.refresh-days:7}") long refreshDays) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.validadeAccess = Duration.ofMinutes(accessMinutes);
        this.validadeRefresh = Duration.ofDays(refreshDays);
    }

    public long segundosAccess() {
        return validadeAccess.getSeconds();
    }

    public String gerarAccessToken(AuthUser u) {
        return gerar(u, "access", validadeAccess);
    }

    public String gerarRefreshToken(AuthUser u) {
        return gerar(u, "refresh", validadeRefresh);
    }

    private String gerar(AuthUser u, String finalidade, Duration validade) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(u.id()))
                .claim("typ", finalidade)
                .claim("tipo", u.tipo())
                .claim("perfil", u.perfil())
                .claim("email", u.email())
                .claim("unidadeId", u.unidadeId() == null ? "" : String.valueOf(u.unidadeId()))
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(validade)))
                .signWith(chave)
                .compact();
    }

    /** Valida assinatura, validade e finalidade ("access" ou "refresh"). Lança JwtException se inválido. */
    public AuthUser validar(String token, String finalidadeEsperada) {
        Claims claims = Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload();
        if (!finalidadeEsperada.equals(claims.get("typ", String.class))) {
            throw new JwtException("Tipo de token inválido");
        }
        String unidade = claims.get("unidadeId", String.class);
        Long unidadeId = (unidade == null || unidade.isBlank()) ? null : Long.valueOf(unidade);
        return new AuthUser(Long.valueOf(claims.getSubject()), claims.get("tipo", String.class),
                claims.get("perfil", String.class), unidadeId, claims.get("email", String.class));
    }
}
