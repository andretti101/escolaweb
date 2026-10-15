package com.andretti101.escolaweb.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /v3/api-docs should be publicly accessible and return valid OpenAPI 3 document")
    void testApiDocsEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        assertNotNull(json);
        assertFalse(json.isBlank());

        JsonNode root = objectMapper.readTree(json);
        assertTrue(root.path("openapi").asText().startsWith("3."), "OpenAPI version should be 3.x");
        assertEquals("EscolaWeb API", root.path("info").path("title").asText());
        assertEquals("1.0.0", root.path("info").path("version").asText());

        // Verify security scheme
        JsonNode securitySchemes = root.path("components").path("securitySchemes");
        assertTrue(securitySchemes.has("bearerAuth"), "bearerAuth security scheme should exist");
        assertEquals("http", securitySchemes.path("bearerAuth").path("type").asText());
        assertEquals("bearer", securitySchemes.path("bearerAuth").path("scheme").asText());
        assertEquals("JWT", securitySchemes.path("bearerAuth").path("bearerFormat").asText());

        // Count operations across paths
        JsonNode paths = root.path("paths");
        assertTrue(paths.isObject(), "paths should be an object");

        int operationCount = 0;
        int pathCount = 0;
        Iterator<Map.Entry<String, JsonNode>> fields = paths.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            pathCount++;
            String path = entry.getKey();
            JsonNode pathItem = entry.getValue();
        // Validate all operations
        for (String method : new String[]{"get", "post", "put", "patch", "delete"}) {
            if (pathItem.has(method)) {
                operationCount++;
                JsonNode op = pathItem.get(method);
                assertNotNull(op.get("summary"), "Operation " + method + " " + path + " should have a summary");
                assertFalse(op.get("summary").asText().isBlank(), "Operation " + method + " " + path + " summary should not be blank");
                assertNotNull(op.get("description"), "Operation " + method + " " + path + " should have a description");
                assertFalse(op.get("description").asText().isBlank(), "Operation " + method + " " + path + " description should not be blank");
                assertTrue(op.has("tags") && op.get("tags").size() > 0, "Operation " + method + " " + path + " should have tags");
                assertTrue(op.has("responses") && op.get("responses").size() > 0, "Operation " + method + " " + path + " should have responses");

                // Verify that Pageable was not left as an unexploded required object param
                if (op.has("parameters")) {
                    for (JsonNode param : op.get("parameters")) {
                        if ("pageable".equals(param.path("name").asText())) {
                            fail("Operation " + method + " " + path + " has raw 'pageable' parameter instead of @ParameterObject");
                        }
                    }
                }
            }
        }
    }

    System.out.println("TOTAL OPENAPI PATHS: " + pathCount);
    System.out.println("TOTAL OPENAPI OPERATIONS: " + operationCount);

    // Verify public auth endpoints have security: []
    for (String publicPath : new String[]{"/auth/login", "/auth/refresh", "/auth/forgot-password", "/auth/reset-password"}) {
        JsonNode sec = paths.path(publicPath).path("post").path("security");
        assertTrue(sec.isArray(), "Public endpoint " + publicPath + " should have security array");
        assertEquals(0, sec.size(), "Public endpoint " + publicPath + " should have empty security array");
    }

    assertTrue(operationCount >= 100, "Should have more than 100 operations documented, found: " + operationCount);
}

@Test
@DisplayName("GET /swagger-ui/index.html should be publicly accessible")
void testSwaggerUiEndpoint() throws Exception {
    mockMvc.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk());
}

@Test
@DisplayName("GET /swagger-ui.html redirect should be accessible")
void testSwaggerUiRedirect() throws Exception {
    mockMvc.perform(get("/swagger-ui.html"))
            .andExpect(status().is3xxRedirection());
}
}
