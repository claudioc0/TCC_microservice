package com.tcc.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.order.security.testsupport.TokenFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração de ponta a ponta do order-service: controller real, service real,
 * repositório real (H2 em memória) e filtro JWT real, todos no mesmo contexto Spring.
 * O único ponto substituído é a fronteira de rede com o product-service — que aqui não
 * está de fato em execução — via {@link MockRestServiceServer} amarrado ao mesmo
 * {@link RestClient.Builder} que o {@code ProductClient} de produção usa. Tudo que está
 * dentro do processo do order-service roda sem mocks.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderIntegrationTest {

    private static final String JWT_SECRET = "3cFqr9Xv8pNzLmKwEoJbDsYtAhUgRiPl2nVxOzBmQkHyWdCu";
    private static final String PRODUCT_SERVICE_BASE_URL = "http://localhost:8082";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MockRestServiceServer productServiceMock;

    /**
     * A amarração do {@link MockRestServiceServer} ao builder precisa acontecer dentro do
     * mesmo método {@code @Bean} que cria o {@link RestClient.Builder} — se fosse um segundo
     * bean dependendo do builder, não haveria garantia de que ele roda antes do
     * {@code ProductClient} (que também depende só do builder) já ter chamado
     * {@code .build()} sobre uma cópia sem o mock instalado.
     */
    @TestConfiguration
    static class ProductServiceMockConfig {

        private MockRestServiceServer mockServer;

        @Bean
        @Primary
        RestClient.Builder testRestClientBuilder() {
            RestClient.Builder builder = RestClient.builder();
            this.mockServer = MockRestServiceServer.bindTo(builder).build();
            return builder;
        }

        @Bean
        MockRestServiceServer productServiceMock(RestClient.Builder testRestClientBuilder) {
            return mockServer;
        }
    }

    private String bearerTokenFor(Long userId, String email, String role) {
        return "Bearer " + new TokenFactory(JWT_SECRET).generate(userId, email, role, 3_600_000L);
    }

    @Test
    @DisplayName("Fluxo completo: criar pedido, transicionar status e consultar — via pilha real (controller→service→repositório H2)")
    void fullOrderLifecycle_ShouldPersistAndTransitionThroughRealStack() throws Exception {
        String customerToken = bearerTokenFor(1L, "cliente@test.com", "CUSTOMER");
        String adminToken = bearerTokenFor(2L, "admin@test.com", "ADMIN");

        productServiceMock.expect(requestTo(PRODUCT_SERVICE_BASE_URL + "/api/products/1"))
                .andRespond(withSuccess("""
                        {"id":1,"name":"Mouse","price":150.00,"stockQuantity":10}""", MediaType.APPLICATION_JSON));
        productServiceMock.expect(requestTo(PRODUCT_SERVICE_BASE_URL + "/api/products/1/stock/decrease"))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withSuccess());

        String createResponseBody = mockMvc.perform(post("/api/orders")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"productId":1,"quantity":2}]}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.totalAmount").value(300.00))
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(createResponseBody).get("id").asLong();

        // GET por id: pedido real, persistido no H2 na chamada anterior — nenhum mock de repositório
        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"));

        // updateStatus é ADMIN-only — verifica @PreAuthorize real, não simulado
        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newStatus":"CONFIRMADO"}"""))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newStatus":"CONFIRMADO"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"));

        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"));

        productServiceMock.verify();
    }

    @Test
    @DisplayName("Requisição sem Authorization deve ser rejeitada pela pilha real de segurança (401/403)")
    void createWithoutToken_ShouldBeRejectedByRealSecurityFilterChain() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"productId":1,"quantity":1}]}"""))
                .andExpect(result -> {
                    int sc = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(sc).isIn(401, 403);
                });
    }
}
