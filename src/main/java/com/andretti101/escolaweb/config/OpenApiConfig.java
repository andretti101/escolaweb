package com.andretti101.escolaweb.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI escolaWebOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EscolaWeb API")
                        .description("Documentação da API RESTful do Sistema de Gestão Escolar EscolaWeb. "
                                + "Esta documentação abrange todos os módulos da aplicação: controle de acesso e autenticação, "
                                + "gestão do ano letivo e períodos acadêmicos, turmas e enturmações, professores e disciplinas, "
                                + "alunos e boletins, diário de classe (aulas, presenças e notas), auditoria histórica, "
                                + "comunicados escolares, comunicação por chat em tempo real e assistente de inteligência artificial.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Suporte EscolaWeb")
                                .email("suporte@escolaweb.com"))
                        .license(new License()
                                .name("Proprietário / Uso Acadêmico")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Insira o token JWT retornado no login. Exemplo: eyJhbGciOi...")));
    }
}
