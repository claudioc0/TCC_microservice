package com.tcc.report.security;

import com.tcc.report.security.testsupport.TokenFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-with-at-least-256-bits-for-hs256-signing";

    private final JwtService jwtService = new JwtService(SECRET);
    private final TokenFactory tokenFactory = new TokenFactory(SECRET);

    @Test
    @DisplayName("Deve extrair o principal correto de um token válido")
    void shouldExtractPrincipalFromValidToken() {
        String token = tokenFactory.generate(7L, "admin@test.com", "ADMIN", 3_600_000L);

        AuthenticatedPrincipal principal = jwtService.extractPrincipal(token);

        assertThat(principal.id()).isEqualTo(7L);
        assertThat(principal.email()).isEqualTo("admin@test.com");
        assertThat(principal.role()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Deve validar como verdadeiro um token válido")
    void shouldValidateValidTokenAsTrue() {
        String token = tokenFactory.generate(1L, "valido@test.com", "ADMIN", 3_600_000L);
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
        String token = new TokenFactory("another-completely-different-secret-key-256-bits-long-value")
                .generate(1L, "hacker@test.com", "ADMIN", 3_600_000L);

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    @DisplayName("Deve validar como falso um token expirado")
    void shouldInvalidateExpiredToken() {
        String token = tokenFactory.generate(1L, "expirado@test.com", "ADMIN", -1000L);
        assertThat(jwtService.isTokenValid(token)).isFalse();
    }
}
