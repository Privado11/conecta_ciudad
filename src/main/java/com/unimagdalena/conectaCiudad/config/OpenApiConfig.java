package com.unimagdalena.conectaCiudad.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI conectaCiudadOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Conecta Ciudad API")
                .description("API para gestión de proyectos, usuarios y autenticación en Conecta Ciudad")
                .version("v1")
                .contact(new Contact()
                    .name("Conecta Ciudad")
                    .email("soporte@conectaciudad.local")))
            .externalDocs(new ExternalDocumentation()
                .description("Documentación adicional")
                .url("https://example.com/docs"))
            .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
            .components(new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                    .name(SECURITY_SCHEME_NAME)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
