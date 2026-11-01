package br.com.raizesdonordeste.domain;

public enum Canal {
    APP, WEB, TOTEM, BALCAO;

    /** Canais digitais exigem pagamento antecipado por padrão. */
    public boolean exigePagamentoAntecipadoPorPadrao() {
        return this == APP || this == WEB;
    }
}
