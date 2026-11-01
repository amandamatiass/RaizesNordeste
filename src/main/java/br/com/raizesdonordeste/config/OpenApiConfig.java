package br.com.raizesdonordeste.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "API Raízes do Nordeste",
        version = "0.1.0",
        description = "Back-end da rede Raízes do Nordeste: pedidos, pagamentos, estoque, cardápio por unidade, "
                + "histórico de operações críticas e programa de fidelidade."))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
