package com.tcc.user.controller;

import com.tcc.user.dto.UserResponse;
import com.tcc.user.exception.UserNotFoundException;
import com.tcc.user.security.AuthenticatedPrincipal;
import com.tcc.user.security.JwtAuthenticationFilter;
import com.tcc.user.security.JwtService;
import com.tcc.user.security.SecurityConfig;
import com.tcc.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes do UserController (slice web + Mockito).
 *
 * Foco: isolar a camada de Controller (UserService é mockado) e exercitar a borda
 * HTTP:
 *   - /me resolve o usuário a partir do principal do JWT, nunca de um id na URL
 *     (um CUSTOMER não consegue ler o perfil de outro);
 *   - as rotas de administração exigem role ADMIN;
 *   - exceções de domínio viram os status HTTP corretos.
 *
 * SecurityConfig e JwtAuthenticationFilter são importados para que a cadeia de
 * segurança real seja exercitada neste slice.
 */
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class UserControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean UserService userService;
    @MockBean JwtService jwtService;

    /** Simula o SecurityContext que o JwtAuthenticationFilter produziria a partir de um token válido. */
    private static RequestPostProcessor as(Long userId, String role) {
        var principal = new AuthenticatedPrincipal(userId, "usuario@tcc.com", role);
        return authentication(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private static UserResponse sampleUser(Long id, String name, String role) {
        return new UserResponse(id, name, "usuario@tcc.com", role, LocalDateTime.now());
    }

    // ---- /me ----

    @Test
    @DisplayName("GET /me deve retornar o usuario do token, sem receber id na requisicao")
    void me_ShouldResolveUserFromToken() throws Exception {
        when(userService.findById(7L)).thenReturn(sampleUser(7L, "Joao", "CUSTOMER"));

        mockMvc.perform(get("/api/users/me").with(as(7L, "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(7)))
                .andExpect(jsonPath("$.name", is("Joao")))
                .andExpect(jsonPath("$.role", is("CUSTOMER")));

        verify(userService).findById(7L);
    }

    @Test
    @DisplayName("GET /me sem autenticacao deve ser barrado antes de chegar ao service")
    void me_WithoutAuthentication_ShouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("GET /me deve retornar 404 quando o usuario do token nao existe mais")
    void me_WhenUserWasDeleted_ShouldReturnNotFound() throws Exception {
        when(userService.findById(7L)).thenThrow(new UserNotFoundException(7L));

        mockMvc.perform(get("/api/users/me").with(as(7L, "CUSTOMER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    // ---- Rotas administrativas ----

    @Test
    @DisplayName("GET / com ADMIN deve listar todos os usuarios")
    void findAll_AsAdmin_ShouldReturnOk() throws Exception {
        when(userService.findAll()).thenReturn(List.of(
                sampleUser(1L, "Admin", "ADMIN"), sampleUser(2L, "Joao", "CUSTOMER")));

        mockMvc.perform(get("/api/users").with(as(1L, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].role", is("ADMIN")));

        verify(userService).findAll();
    }

    @Test
    @DisplayName("GET / com CUSTOMER deve retornar 403 sem chamar o service")
    void findAll_AsCustomer_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/users").with(as(7L, "CUSTOMER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("GET /{id} com ADMIN deve retornar o usuario solicitado")
    void findById_AsAdmin_ShouldReturnOk() throws Exception {
        when(userService.findById(2L)).thenReturn(sampleUser(2L, "Joao", "CUSTOMER"));

        mockMvc.perform(get("/api/users/2").with(as(1L, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(2)));

        verify(userService).findById(2L);
    }

    @Test
    @DisplayName("GET /{id} com CUSTOMER deve retornar 403 (nao pode ler o perfil de outro)")
    void findById_AsCustomer_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/users/2").with(as(7L, "CUSTOMER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("GET /{id} com ADMIN deve retornar 404 para id inexistente")
    void findById_WhenUserDoesNotExist_ShouldReturnNotFound() throws Exception {
        when(userService.findById(99L)).thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(get("/api/users/99").with(as(1L, "ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("DELETE /{id} com ADMIN deve retornar 204 sem corpo")
    void delete_AsAdmin_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/users/2").with(as(1L, "ADMIN")))
                .andExpect(status().isNoContent());

        verify(userService).delete(2L);
    }

    @Test
    @DisplayName("DELETE /{id} com CUSTOMER deve retornar 403 sem chamar o service")
    void delete_AsCustomer_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(delete("/api/users/2").with(as(7L, "CUSTOMER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("DELETE /{id} com ADMIN deve retornar 404 para id inexistente")
    void delete_WhenUserDoesNotExist_ShouldReturnNotFound() throws Exception {
        org.mockito.Mockito.doThrow(new UserNotFoundException(99L)).when(userService).delete(99L);

        mockMvc.perform(delete("/api/users/99").with(as(1L, "ADMIN")))
                .andExpect(status().isNotFound());
    }
}
