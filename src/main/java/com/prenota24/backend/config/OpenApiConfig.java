package com.prenota24.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Prenota24 API")
                        .version("1.0.0")
                        .description("""
                                API per la gestione di appuntamenti, eventi e professionisti.
                                
                                **Autenticazione**: JWT Bearer token.
                                Ottieni il token via `POST /api/auth/login` o `POST /api/auth/register`,
                                poi inseriscilo nel pulsante **Authorize** in alto a destra.
                                
                                **Endpoint pubblici** (senza auth): tutto sotto `/api/public/**` e `/api/auth/**`.
                                """)
                        .contact(new Contact()
                                .name("Prenota24")
                                .email("support@prenota24.it")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Locale"),
                        new Server().url("https://api.prenota24.it").description("Produzione")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Inserire il JWT ottenuto dal login. Esempio: `eyJhbGci...`")));
    }
}

