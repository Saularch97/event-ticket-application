package com.example.booking.circuitbreaker;

import com.example.booking.dto.PaymentRequestDto;
import com.example.booking.services.client.PaymentClient;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@WireMockTest(httpPort = 8083)
class PaymentCircuitBreakerTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class TestControllerConfig {
        @RestController
        static class CircuitBreakerTestController {
            private final PaymentClient paymentClient;

            public CircuitBreakerTestController(PaymentClient paymentClient) {
                this.paymentClient = paymentClient;
            }

            @PostMapping("/test/circuit-breaker")
            public String testCall(@RequestBody PaymentRequestDto dto) {
                return paymentClient.getCheckoutUrl(dto);
            }
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("services.payment-processor.url", () -> "http://localhost:8083");
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.openfeign.circuitbreaker.enabled", () -> "true");

        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");

        registry.add("spring.rabbitmq.listener.simple.auto-startup", () -> "false");
    }

    @Test
    @WithMockUser
    void shouldReturn503_WhenPaymentServiceIsDown() throws Exception {
        // Mock do Wiremock retornando erro 500
        stubFor(WireMock.post(urlEqualTo("/payments/checkout"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        String requestJson = """
            {
                "orderId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
                "totalAmount": 100.00,
                "userEmail": "teste@email.com",
                "items": []
            }
        """;

        mockMvc.perform(post("/test/circuit-breaker")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Payment service unavailable. Try again later"));
    }

    @Test
    @WithMockUser
    void shouldReturn503_WhenPaymentServiceTimesOut() throws Exception {
        stubFor(WireMock.post(urlEqualTo("/payments/checkout"))
                .willReturn(aResponse()
                        .withFixedDelay(5000)
                        .withStatus(200)));

        String requestJson = """
            { "orderId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11", "totalAmount": 100.00, "items": [], "userEmail": "t@t.com" }
        """;

        mockMvc.perform(post("/test/circuit-breaker")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isServiceUnavailable());
    }
}
