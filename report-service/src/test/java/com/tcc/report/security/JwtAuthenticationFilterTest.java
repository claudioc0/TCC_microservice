package com.tcc.report.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock JwtService jwtService;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock FilterChain filterChain;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Sem header Authorization: não autentica e continua a cadeia")
    void noAuthorizationHeader_continuesChainWithoutAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Header sem prefixo Bearer: não autentica e continua a cadeia")
    void headerNotBearer_continuesChainWithoutAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Token válido: autentica no contexto de segurança com a role correta")
    void validToken_setsAuthenticationInContext() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valido");
        when(jwtService.isTokenValid("token-valido")).thenReturn(true);
        when(jwtService.extractPrincipal("token-valido"))
                .thenReturn(new AuthenticatedPrincipal(1L, "maria@test.com", "ADMIN"));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isInstanceOf(AuthenticatedPrincipal.class);
        assertThat(((AuthenticatedPrincipal) auth.getPrincipal()).email()).isEqualTo("maria@test.com");
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Token inválido: não autentica e continua a cadeia")
    void invalidToken_doesNotSetAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-invalido");
        when(jwtService.isTokenValid("token-invalido")).thenReturn(false);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractPrincipal(anyString());
    }
}
