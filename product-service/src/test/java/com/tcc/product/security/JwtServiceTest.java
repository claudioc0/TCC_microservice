package com.tcc.product.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-with-at-least-256-bits-for-hs256-signing";

    private final JwtService jwtService = new JwtService(SECRET);

    private String tokenFromUserService(Long userId, String email, String role, long expirationMs) {
        return new com.tcc.product.security.testsupport.TokenFactory(SECRET)
                .generate(userId, email, role, expirationMs);
    }

    @Test
    @DisplayName("Deve extrair o principal correto de um token válido")
    void shouldExtractPrincipalFromValidToken() {
        String token = tokenFromUserService(7L, "joao@test.com", "CUSTOMER", 3_600_000L);

        AuthenticatedPrincipal principal = jwtService.extractPrincipal(token);

        assertThat(principal.id()).isEqualTo(7L);
        assertThat(principal.email()).isEqualTo("joao@test.com");
        assertThat(principal.role()).isEqualTo("CUSTOMER");
    }

    @Test
    @DisplayName("Deve validar como verdadeiro um token válido")
    void shouldValidateValidTokenAsTrue() {
        String token = tokenFromUserService(1L, "valido@test.com", "ADMIN", 3_600_000L);
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("Deve validar como falso um token malformado")
    void shouldInvalidateMalformedToken() {
        assertThat(jwtService.isTokenValid("token-invalido")).isFalse();
    }

    @Test
    @DisplayName("Deve validar como falso um token assinado com outra chave")
    void shouldInvalidateTokenSignedWithDifferentKey() {
        String token = new com.tcc.product.security.testsupport.TokenFactory(
                "another-completely-different-secret-key-256-bits-long-value")
                .generate(1L, "hacker@test.com", "ADMIN", 3_600_000L);

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    @DisplayName("Deve validar como falso um token expirado")
    void shouldInvalidateExpiredToken() {
        String token = tokenFromUserService(1L, "expirado@test.com", "CUSTOMER", -1000L);
        assertThat(jwtService.isTokenValid(token)).isFalse();
    }
}
