package com.sagant.notifications.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${notifications.security.api-key}")
    private String apiKey;

    @Test
    void creaUnaNotificacionValidaYQuedaEnEstadoPending() throws Exception {
        Map<String, Object> request = Map.of(
                "recipient", "ops-team",
                "channel", "LOG",
                "subject", "Test de integracion",
                "body", "Cuerpo del mensaje",
                "priority", "HIGH",
                "metadata", Map.of("origin", "integration-test")
        );

        mockMvc.perform(post("/api/v1/notifications")
                        .header("X-API-Key", apiKey)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.channel").value("LOG"));
    }

    @Test
    void rechazaLaCreacionSinApiKey() throws Exception {
        Map<String, Object> request = Map.of(
                "recipient", "ops-team",
                "channel", "LOG",
                "subject", "Test",
                "body", "Body",
                "priority", "LOW"
        );

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rechazaUnaSolicitudSinCamposObligatoriosConMensajesDescriptivos() throws Exception {
        Map<String, Object> requestIncompleto = Map.of(
                "recipient", "ops-team"
        );

        mockMvc.perform(post("/api/v1/notifications")
                        .header("X-API-Key", apiKey)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestIncompleto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages", notNullValue()));
    }

    @Test
    void permiteConsultarUnaNotificacionCreadaPorId() throws Exception {
        Map<String, Object> request = Map.of(
                "recipient", "ops-team",
                "channel", "LOG",
                "subject", "Consulta por ID",
                "body", "Body",
                "priority", "MEDIUM"
        );

        String response = mockMvc.perform(post("/api/v1/notifications")
                        .header("X-API-Key", apiKey)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/v1/notifications/" + id)
                        .header("X-API-Key", apiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }
}