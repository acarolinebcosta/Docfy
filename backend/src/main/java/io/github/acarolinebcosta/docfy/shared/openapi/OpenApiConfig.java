package io.github.acarolinebcosta.docfy.shared.openapi;

import io.github.acarolinebcosta.docfy.shared.error.ApiErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI docfyOpenApi() {
        Components components = new Components().addSecuritySchemes(
                BEARER_AUTH,
                new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
        );

        ModelConverters.getInstance()
                .read(ApiErrorResponse.class)
                .forEach(components::addSchemas);

        addErrorResponse(
                components,
                "BadRequest",
                "The request is invalid"
        );
        addErrorResponse(
                components,
                "Unauthorized",
                "Authentication is required"
        );
        addErrorResponse(
                components,
                "Forbidden",
                "The authenticated user cannot perform the operation"
        );
        addErrorResponse(
                components,
                "NotFound",
                "The resource is missing or concealed"
        );
        addErrorResponse(
                components,
                "Conflict",
                "The resource state does not allow the operation"
        );

        return new OpenAPI()
                .info(
                        new Info()
                                .title("Docfy API")
                                .version("v1")
                                .description(
                                        "Document management API with JWT authentication, "
                                                + "authorization-aware discovery, workflow, "
                                                + "audit trail and file attachments."
                                )
                )
                .components(components);
    }

    private void addErrorResponse(
            Components components,
            String name,
            String description
    ) {
        Schema<?> schema = new Schema<>()
                .$ref("#/components/schemas/ApiErrorResponse");
        Content content = new Content().addMediaType(
                org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                new MediaType().schema(schema)
        );

        components.addResponses(
                name,
                new ApiResponse()
                        .description(description)
                        .content(content)
        );
    }
}
