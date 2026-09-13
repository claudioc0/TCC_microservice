package com.tcc.user;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Teste de integração de ponta a ponta do user-service: controller real, service real,
 * {@code PasswordEncoder} real (BCrypt), repositório real (H2 em memória) e emissão/validação
 * real de JWT no mesmo contexto Spring — sem mocks. Cobre o único fluxo deste serviço que os
 * outros três não têm: cadastro + autenticação com senha.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("Fluxo completo: registrar, logar, acessar recurso próprio e ser bloqueado de recurso ADMIN — via pilha real")
    void fullAuthLifecycle_ShouldRegisterLoginAndEnforceAuthorizationThroughRealStack() throws Exception {
        String registerBody = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Maria","email":"maria.integracao@test.com","password":"senha123"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andReturn().getResponse().getContentAsString();

        Long userId = objectMapper.readTree(registerBody).get("userId").asLong();

        // Registrar de novo com o mesmo e-mail deve falhar — restrição real de unicidade, não simulada
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Maria","email":"maria.integracao@test.com","password":"outrasenha"}"""))
                .andExpect(status().isConflict());

        // Login com senha errada deve falhar — BCrypt real comparando hash, não um stub
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"maria.integracao@test.com","password":"senhaerrada"}"""))
                .andExpect(status().isUnauthorized());

        String loginBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"maria.integracao@test.com","password":"senha123"}"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String customerToken = "Bearer " + objectMapper.readTree(loginBody).get("token").asText();

        mockMvc.perform(get("/api/users/me").header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("maria.integracao@test.com"));

        // Endpoint ADMIN-only bloqueado para CUSTOMER — @PreAuthorize real
        mockMvc.perform(get("/api/users").header("Authorization", customerToken))
                .andExpect(status().isForbidden());

        // Admin semeado pelo DatabaseSeeder — login real, não fixture de teste
        String adminLoginBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@tcc.com","password":"admin123"}"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String adminToken = "Bearer " + objectMapper.readTree(adminLoginBody).get("token").asText();

        mockMvc.perform(get("/api/users").header("Authorization", adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/users/{id}", userId).header("Authorization", adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/{id}", userId).header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }
}
