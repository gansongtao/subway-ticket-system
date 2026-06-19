package com.subway.ticket.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.subway.ticket.service.QrSignService;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Sql("/sql/test-data.sql")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class KioskControllerTest extends AbstractIntegrationTest {

    private final QrSignService qrSignService = new QrSignService("dev-secret");

    /** Create order → pay → generate QR → return the QR payload as a JsonNode */
    private JsonNode getQrPayload() throws Exception {
        var createReq = new com.subway.ticket.dto.CreateOrderReq();
        createReq.setFrom("S001");
        createReq.setTo("S005");

        String orderResp = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(orderResp).get("id").asLong();

        String payBody = objectMapper.writeValueAsString(
                objectMapper.createObjectNode().put("orderId", orderId));
        mockMvc.perform(post("/api/payments/mock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payBody))
                .andExpect(status().isOk());

        String qrResp = mockMvc.perform(get("/api/orders/{id}/qrcode", orderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(qrResp);
    }

    @Test
    void shouldValidateValidQrCode() throws Exception {
        JsonNode qr = getQrPayload();

        String body = objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .put("orderId", qr.get("orderId").asLong())
                .put("nonce", qr.get("nonce").asText())
                .put("exp", qr.get("exp").asLong())
                .put("sign", qr.get("sign").asText()));

        mockMvc.perform(post("/api/kiosk/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.reason").value("OK"));
    }

    @Test
    void shouldRejectTamperedSignature() throws Exception {
        JsonNode qr = getQrPayload();

        String body = objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .put("orderId", qr.get("orderId").asLong())
                .put("nonce", qr.get("nonce").asText())
                .put("exp", qr.get("exp").asLong())
                .put("sign", qr.get("sign").asText() + "tampered"));

        mockMvc.perform(post("/api/kiosk/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.reason").value("SIGN_INVALID"));
    }

    @Test
    void shouldRejectExpiredQrCode() throws Exception {
        JsonNode qr = getQrPayload();
        Long orderId = qr.get("orderId").asLong();

        // Generate a valid signature for an already-expired payload
        long pastExp = 1000000L;  // Past epoch (year 2001)
        String data = orderId + ":" + qr.get("nonce").asText() + ":" + pastExp;
        String validSignForExpired = qrSignService.sign(data);

        String body = objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .put("orderId", orderId)
                .put("nonce", qr.get("nonce").asText())
                .put("exp", pastExp)
                .put("sign", validSignForExpired));

        mockMvc.perform(post("/api/kiosk/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.reason").value("EXPIRED"));
    }

    @Test
    void shouldIssueTicketSuccessfully() throws Exception {
        JsonNode qr = getQrPayload();

        String body = objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .put("orderId", qr.get("orderId").asLong())
                .put("nonce", qr.get("nonce").asText())
                .put("exp", qr.get("exp").asLong())
                .put("sign", qr.get("sign").asText()));

        mockMvc.perform(post("/api/tickets/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issued").value(true))
                .andExpect(jsonPath("$.ticket.fromStation").isNotEmpty())
                .andExpect(jsonPath("$.ticket.toStation").isNotEmpty())
                .andExpect(jsonPath("$.ticket.price").isNumber());
    }
}