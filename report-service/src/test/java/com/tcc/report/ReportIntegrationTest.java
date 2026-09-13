package com.tcc.report;

import com.tcc.report.security.testsupport.TokenFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração de ponta a ponta do report-service: controller real e service real
 * (agregação de verdade) no mesmo contexto Spring. Este serviço não tem banco próprio —
 * toda a fronteira externa é a chamada HTTP ao order-service, que é o único ponto
 * substituído aqui, via {@link MockRestServiceServer} amarrado ao mesmo
 * {@link RestClient.Builder} que o {@code OrderClient} de produção usa.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReportIntegrationTest {

    private static final String JWT_SECRET = "3cFqr9Xv8pNzLmKwEoJbDsYtAhUgRiPl2nVxOzBmQkHyWdCu";
    private static final String ORDER_SERVICE_BASE_URL = "http://localhost:8083";

    @Autowired MockMvc mockMvc;
    @Autowired MockRestServiceServer orderServiceMock;

    /**
     * Mesma técnica usada em OrderIntegrationTest: a amarração do MockRestServiceServer
     * precisa acontecer dentro do próprio método @Bean que cria o builder, para garantir
     * que ela existe antes do OrderClient (que também depende só do builder) chamar
     * .build() sobre ele.
     */
    @TestConfiguration
    static class OrderServiceMockConfig {

        private MockRestServiceServer mockServer;

        @Bean
        @Primary
        RestClient.Builder testRestClientBuilder() {
            RestClient.Builder builder = RestClient.builder();
            this.mockServer = MockRestServiceServer.bindTo(builder).build();
            return builder;
        }

        @Bean
        MockRestServiceServer orderServiceMock(RestClient.Builder testRestClientBuilder) {
            return mockServer;
        }
    }

    private String bearerTokenFor(Long userId, String email, String role) {
        return "Bearer " + new TokenFactory(JWT_SECRET).generate(userId, email, role, 3_600_000L);
    }

    @Test
    @DisplayName("Relatório de vendas deve agregar pedidos reais do order-service e exigir ADMIN — via pilha real")
    void salesReport_ShouldAggregateOrdersFromOrderServiceThroughRealStack() throws Exception {
        String adminToken = bearerTokenFor(1L, "admin@test.com", "ADMIN");
        String customerToken = bearerTokenFor(2L, "cliente@test.com", "CUSTOMER");

        orderServiceMock.expect(requestTo(ORDER_SERVICE_BASE_URL
                        + "/api/orders?startDate=2024-01-01&endDate=2024-01-31"))
                .andRespond(withSuccess("""
                        [
                          {"id":1,"userId":10,"totalAmount":300.00,"status":"ENTREGUE","createdAt":"2024-01-05T10:00:00"},
                          {"id":2,"userId":11,"totalAmount":150.00,"status":"CANCELADO","createdAt":"2024-01-10T10:00:00"},
                          {"id":3,"userId":12,"totalAmount":500.00,"status":"PENDENTE","createdAt":"2024-01-15T10:00:00"}
                        ]""", MediaType.APPLICATION_JSON));

        // Relatório é ADMIN-only — @PreAuthorize de classe real, não simulado
        mockMvc.perform(get("/api/reports/sales")
                        .header("Authorization", customerToken)
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/reports/sales")
                        .header("Authorization", adminToken)
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(3))
                .andExpect(jsonPath("$.deliveredOrders").value(1))
                .andExpect(jsonPath("$.canceledOrders").value(1))
                // receita soma tudo que não é CANCELADO: 300.00 (ENTREGUE) + 500.00 (PENDENTE)
                .andExpect(jsonPath("$.totalRevenue").value(800.00));

        orderServiceMock.verify();
    }

    @Test
    @DisplayName("Período inválido (início após fim) deve ser rejeitado pela regra real de negócio")
    void salesReport_WithStartAfterEnd_ShouldReturnBadRequest() throws Exception {
        String adminToken = bearerTokenFor(1L, "admin@test.com", "ADMIN");

        mockMvc.perform(get("/api/reports/sales")
                        .header("Authorization", adminToken)
                        .param("startDate", "2024-02-01")
                        .param("endDate", "2024-01-01"))
                .andExpect(status().isBadRequest());
    }
}
