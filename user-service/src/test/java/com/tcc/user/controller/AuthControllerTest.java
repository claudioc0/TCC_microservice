package com.tcc.user.controller;

import com.tcc.user.dto.AuthResponse;
import com.tcc.user.dto.LoginRequest;
import com.tcc.user.dto.RegisterRequest;
import com.tcc.user.exception.DuplicateEmailException;
import com.tcc.user.exception.InvalidCredentialsException;
import com.tcc.user.security.JwtAuthenticationFilter;
import com.tcc.user.security.JwtService;
import com.tcc.user.security.SecurityConfig;
import com.tcc.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean UserService userService;
    @MockBean JwtService jwtService;

    @Test
    @DisplayName("Deve retornar 201 e o token ao registrar com dados válidos")
    void register_WithValidData_ShouldReturnCreated() throws Exception {
        var request = new RegisterRequest("Maria", "maria@test.com", "senha123");
        when(userService.register(request))
                .thenReturn(AuthResponse.of("token-abc", 1L, "Maria", "maria@test.com", "CUSTOMER"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", org.hamcrest.Matchers.is("token-abc")))
                .andExpect(jsonPath("$.role", org.hamcrest.Matchers.is("CUSTOMER")));
    }

    @Test
    @DisplayName("Deve retornar 400 ao registrar com e-mail inválido")
    void register_WithInvalidEmail_ShouldReturnBadRequest() throws Exception {
        var request = new RegisterRequest("Maria", "nao-e-email", "senha123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar 409 ao registrar e-mail já cadastrado")
    void register_WithDuplicateEmail_ShouldReturnConflict() throws Exception {
        var request = new RegisterRequest("Maria", "maria@test.com", "senha123");
        when(userService.register(request)).thenThrow(new DuplicateEmailException("maria@test.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Deve retornar 200 e o token ao logar com credenciais válidas")
    void login_WithValidCredentials_ShouldReturnOk() throws Exception {
        var request = new LoginRequest("maria@test.com", "senha123");
        when(userService.login(request))
                .thenReturn(AuthResponse.of("token-xyz", 1L, "Maria", "maria@test.com", "CUSTOMER"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", org.hamcrest.Matchers.is("token-xyz")));
    }

    @Test
    @DisplayName("Deve retornar 401 ao logar com credenciais inválidas")
    void login_WithInvalidCredentials_ShouldReturnUnauthorized() throws Exception {
        var request = new LoginRequest("maria@test.com", "errada");
        when(userService.login(request)).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
