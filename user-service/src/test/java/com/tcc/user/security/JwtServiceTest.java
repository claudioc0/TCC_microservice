package com.tcc.user.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "test-secret-key-with-at-least-256-bits-for-hs256-signing", 3_600_000L);

    @Test
    @DisplayName("Deve gerar um token não vazio")
    void shouldGenerateNonEmptyToken() {
        String token = jwtService.generateToken(1L, "maria@test.com", "ADMIN");
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("Deve extrair o principal correto do token gerado")
    void shouldExtractPrincipalFromToken() {
        String token = jwtService.generateToken(7L, "joao@test.com", "CUSTOMER");

        AuthenticatedPrincipal principal = jwtService.extractPrincipal(token);

        assertThat(principal.id()).isEqualTo(7L);
        assertThat(principal.email()).isEqualTo("joao@test.com");
        assertThat(principal.role()).isEqualTo("CUSTOMER");
    }

    @Test
    @DisplayName("Deve validar como verdadeiro um token gerado corretamente")
    void shouldValidateGeneratedTokenAsTrue() {
        String token = jwtService.generateToken(1L, "valido@test.com", "CUSTOMER");
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("Deve validar como falso um token malformado")
    void shouldInvalidateMalformedToken() {
        assertThat(jwtService.isTokenValid("token-invalido-nao-assinado")).isFalse();
    }

    @Test
    @DisplayName("Deve validar como falso um token assinado com outra chave")
    void shouldInvalidateTokenSignedWithDifferentKey() {
        JwtService otherService = new JwtService(
                "another-completely-different-secret-key-256-bits-long-value", 3_600_000L);
        String token = otherService.generateToken(1L, "hacker@test.com", "ADMIN");

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    @DisplayName("Deve validar como falso um token expirado")
    void shouldInvalidateExpiredToken() {
        JwtService expiredService = new JwtService(
                "test-secret-key-with-at-least-256-bits-for-hs256-signing", -1000L);
        String token = expiredService.generateToken(1L, "expirado@test.com", "CUSTOMER");

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }
}
