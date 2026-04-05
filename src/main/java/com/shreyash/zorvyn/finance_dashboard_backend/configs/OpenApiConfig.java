package com.shreyash.zorvyn.finance_dashboard_backend.configs;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 3 configuration.
 *
 * Swagger UI is available at: http://localhost:8080/swagger-ui.html
 * OpenAPI JSON spec at:       http://localhost:8080/v3/api-docs
 *
 * A global Bearer JWT security scheme is declared here so that
 * the "Authorize" button appears at the top of Swagger UI.
 * Individual endpoints can opt out via @SecurityRequirements(value = {}).
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title       = "Finance Dashboard API",
                version     = "0.0.1",
                description = """
            Production-grade REST API for a Finance Dashboard system.
            Supports JWT-based authentication with role-based access control
            (VIEWER, ANALYST, ADMIN).
            
            **Quick start:**
            1. POST /api/auth/login with admin@finance.com / Admin@123
            2. Copy the returned `token`
            3. Click **Authorize** and enter: `Bearer <token>`
            """,
                contact = @Contact(
                        name  = "Finance Dashboard Team",
                        email = "support@finance.com"
                ),
                license = @License(name = "MIT")
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local development"),
                @Server(url = "https://api.finance.com", description = "Production")
        },
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name         = "bearerAuth",
        type         = SecuritySchemeType.HTTP,
        scheme       = "bearer",
        bearerFormat = "JWT",
        in           = SecuritySchemeIn.HEADER,
        description  = "Enter your JWT token obtained from POST /api/auth/login"
)
public class OpenApiConfig {
    // configuration is supplied via annotations above.
}
