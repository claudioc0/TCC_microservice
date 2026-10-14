package com.tcc.report.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários do GlobalExceptionHandler.
 *
 * O handler é exercitado diretamente, sem contexto Spring: o contrato testado é
 * "exceção → status HTTP + corpo de erro". Vale notar a distinção que o
 * report-service precisa fazer: erro do cliente (período inválido → 400) versus
 * indisponibilidade do order-service, de quem ele depende (→ 502).
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("IllegalArgumentException (período inválido) deve virar 400 Bad Request")
    void handleIllegalArgument_ShouldReturnBadRequest() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleIllegalArgument(
                new IllegalArgumentException("A data de início não pode ser posterior à data de fim."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message()).isEqualTo("A data de início não pode ser posterior à data de fim.");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Falha de comunicação com o order-service deve virar 502 Bad Gateway")
    void handleOrderServiceUnavailable_ShouldReturnBadGateway() {
        var ex = new OrderServiceUnavailableException(
                "Falha ao consultar pedidos no order-service para o período informado.",
                new RestClientException("connection refused"));

        var response = handler.handleOrderServiceUnavailable(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(502);
        assertThat(response.getBody().message())
                .isEqualTo("Falha ao consultar pedidos no order-service para o período informado.");
    }

    @Test
    @DisplayName("AccessDeniedException deve virar 403 sem vazar detalhes internos")
    void handleAccessDenied_ShouldReturnForbiddenWithGenericMessage() {
        var response = handler.handleAccessDenied(
                new AccessDeniedException("Access Denied: PreAuthorize[hasRole ADMIN]"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Acesso negado.");
    }
}
