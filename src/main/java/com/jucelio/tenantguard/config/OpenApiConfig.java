package com.jucelio.tenantguard.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI tenantGuardOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TenantGuard Java API")
                        .version("1.0.0")
                        .description("""
                                Secure Multi-Tenant SaaS API built with
                                Java 21, Spring Boot, JWT and PostgreSQL RLS.

                                Tenant isolation is enforced at both the
                                application and database layers.
                                """))
                .components(new Components()
                        .addSecuritySchemes(
                                SECURITY_SCHEME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        ));
    }
}