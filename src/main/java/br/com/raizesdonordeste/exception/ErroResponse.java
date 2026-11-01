package br.com.raizesdonordeste.exception;

import java.time.Instant;
import java.util.List;

public record ErroResponse(int status, String erro, String mensagem, List<String> detalhes, Instant timestamp) {
}
