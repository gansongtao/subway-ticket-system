package com.subway.ticket.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Sql("/sql/test-data.sql")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EndToEndTest extends AbstractIntegrationTest {

    @Test
    void fullTicketFlowShouldSucceed() throws Exception {
        // Step 1: Get fare quote
        mockMvc.perform(get("/api/fares/quote")
                        .param("from", "S001")
                        .param("to", "S005")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").isNumber())
                .andExpect(jsonPath("$.path").isArray());

        // Step 2: Create order
        var createReq = new com.subway.ticket.dto.CreateOrderReq();
        createReq.setFrom("S001");
        createReq.setTo("S005");

        String orderResp = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andReturn().getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(orderResp).get("id").asLong();

        // Step 3: Mock payment
        String payBody = objectMapper.writeValueAsString(
                objectMapper.createObjectNode().put("orderId", orderId));
        mockMvc.perform(post("/api/payments/mock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // Step 4: Generate QR code
        String qrResp = mockMvc.perform(get("/api/orders/{id}/qrcode", orderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sign").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        JsonNode qr = objectMapper.readTree(qrResp);

        // Step 5: Validate QR at kiosk
        String validateBody = objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .put("orderId", qr.get("orderId").asLong())
                .put("nonce", qr.get("nonce").asText())
                .put("exp", qr.get("exp").asLong())
                .put("sign", qr.get("sign").asText()));

        mockMvc.perform(post("/api/kiosk/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        // Step 6: Issue ticket
        mockMvc.perform(post("/api/tickets/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issued").value(true))
                .andExpect(jsonPath("$.ticket.fromStation").value("武林广场"))
                .andExpect(jsonPath("$.ticket.toStation").value("建国北路"))
                .andExpect(jsonPath("$.ticket.price").value(3.00));
    }
}
