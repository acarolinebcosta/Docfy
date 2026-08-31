package io.github.acarolinebcosta.docfy.shared.openapi;

import io.github.acarolinebcosta.docfy.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiIntegrationTest implements PostgresTestContainer {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeActualApiContractAndJwtSchemeWithoutAuthentication()
            throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Docfy API"))
                .andExpect(
                        jsonPath(
                                "$.components.securitySchemes.bearerAuth.type"
                        ).value("http")
                )
                .andExpect(
                        jsonPath("$.components.schemas.ApiErrorResponse")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.components.responses.Unauthorized")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.paths['/api/v1/auth/login'].post").exists()
                )
                .andExpect(
                        jsonPath("$.paths['/api/v1/categories'].get").exists()
                )
                .andExpect(
                        jsonPath("$.paths['/api/v1/documents'].get").exists()
                )
                .andExpect(
                        jsonPath(
                                "$.paths['/api/v1/documents'].get.parameters[?(@.name == 'search')]"
                        ).exists()
                )
                .andExpect(
                        jsonPath(
                                "$.paths['/api/v1/documents'].get.parameters[?(@.name == 'categoryId')]"
                        ).exists()
                )
                .andExpect(
                        jsonPath(
                                "$.paths['/api/v1/documents'].get.parameters[?(@.name == 'status')]"
                        ).exists()
                )
                .andExpect(
                        jsonPath("$.paths['/api/v1/documents/{id}/audit'].get")
                                .exists()
                )
                .andExpect(
                        jsonPath(
                                "$.paths['/api/v1/documents/{documentId}/files'].post"
                        ).exists()
                )
                .andExpect(
                        jsonPath(
                                "$.paths['/api/v1/documents/{documentId}/files'].post.requestBody.content['multipart/form-data']"
                        ).exists()
                )
                .andExpect(
                        jsonPath(
                                "$.paths['/api/v1/documents/{documentId}/files/{fileId}'].get"
                        ).exists()
                );
    }

    @Test
    void shouldExposeSwaggerUiWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
