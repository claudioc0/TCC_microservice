package com.tcc.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.product.security.testsupport.TokenFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração de ponta a ponta do product-service: controller real, service real,
 * repositório real (H2 em memória) e filtro JWT real no mesmo contexto Spring — sem mocks,
 * já que este serviço não depende de nenhum outro (fronteira de rede não entra em jogo aqui).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProductIntegrationTest {

    private static final String JWT_SECRET = "3cFqr9Xv8pNzLmKwEoJbDsYtAhUgRiPl2nVxOzBmQkHyWdCu";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String bearerTokenFor(Long userId, String email, String role) {
        return "Bearer " + new TokenFactory(JWT_SECRET).generate(userId, email, role, 3_600_000L);
    }

    @Test
    @DisplayName("Fluxo completo: criar (ADMIN), ler (público), ajustar estoque e apagar — via pilha real")
    void fullProductLifecycle_ShouldPersistAndEnforceAuthorizationThroughRealStack() throws Exception {
        String adminToken = bearerTokenFor(1L, "admin@test.com", "ADMIN");
        String customerToken = bearerTokenFor(2L, "cliente@test.com", "CUSTOMER");

        String createBody = mockMvc.perform(post("/api/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Teclado","description":"Mecânico","price":250.00,"stockQuantity":10}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Teclado"))
                .andExpect(jsonPath("$.stockQuantity").value(10))
                .andReturn().getResponse().getContentAsString();

        Long productId = objectMapper.readTree(createBody).get("id").asLong();

        // Criação exige ADMIN — verifica @PreAuthorize real, não simulado
        mockMvc.perform(post("/api/products")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mouse","description":"Sem fio","price":100.00,"stockQuantity":5}"""))
                .andExpect(status().isForbidden());

        // Leitura é pública — nenhum header de autenticação
        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Teclado"));

        // Ajuste de estoque exige apenas autenticação (não ADMIN) — chamado pelo order-service
        mockMvc.perform(patch("/api/products/{id}/stock/decrease", productId)
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity":3}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(7));

        mockMvc.perform(patch("/api/products/{id}/stock/decrease", productId)
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity":999}"""))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(delete("/api/products/{id}", productId)
                        .header("Authorization", adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isNotFound());
    }
}
