package com.tcc.order.exception;

import com.tcc.order.entity.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do GlobalExceptionHandler (JUnit 5 + Mockito).
 *
 * Aqui o handler é exercitado diretamente, sem subir contexto Spring: o contrato
 * testado é "exceção de domínio → status HTTP + corpo de erro". Isso cobre também
 * os mapeamentos que os testes de controller não conseguem provocar sozinhos
 * (ex.: falha HTTP do product-service virando 502).
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Mock MethodArgumentNotValidException validationException;

    @Test
    @DisplayName("OrderNotFoundException deve virar 404 com a mensagem da exceção")
    void handleOrderNotFound_ShouldReturnNotFound() {
        var ex = new OrderNotFoundException(99L);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleOrderNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().message()).isEqualTo("Pedido não encontrado com id: 99");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("InvalidStatusTransitionException deve virar 422")
    void handleInvalidStatusTransition_ShouldReturnUnprocessableEntity() {
        var ex = new InvalidStatusTransitionException(1L, OrderStatus.ENTREGUE, OrderStatus.PENDENTE);

        var response = handler.handleInvalidStatusTransition(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(422);
        assertThat(response.getBody().message()).contains("ENTREGUE", "PENDENTE");
    }

    @Test
    @DisplayName("ProductNotFoundException deve virar 422 (produto inexistente no product-service)")
    void handleProductNotFound_ShouldReturnUnprocessableEntity() {
        var response = handler.handleProductNotFound(new ProductNotFoundException(7L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("7");
    }

    @Test
    @DisplayName("InsufficientStockException deve virar 422")
    void handleInsufficientStock_ShouldReturnUnprocessableEntity() {
        var response = handler.handleInsufficientStock(new InsufficientStockException(7L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(422);
    }

    @Test
    @DisplayName("Falha de comunicação com o product-service deve virar 502 Bad Gateway")
    void handleProductServiceUnavailable_ShouldReturnBadGateway() {
        var ex = new ProductServiceUnavailableException(
                "Falha ao comunicar com o product-service.", new RestClientException("connection refused"));

        var response = handler.handleProductServiceUnavailable(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(502);
        assertThat(response.getBody().message()).isEqualTo("Falha ao comunicar com o product-service.");
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

    @Test
    @DisplayName("Erros de validação devem virar 400 com o mapa campo → mensagem")
    void handleValidationErrors_ShouldReturnBadRequestWithFieldMap() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "orderRequest");
        bindingResult.addError(new FieldError("orderRequest", "items", "O pedido deve conter pelo menos um item."));
        bindingResult.addError(new FieldError("orderRequest", "items[0].quantity", "A quantidade deve ser no mínimo 1."));
        when(validationException.getBindingResult()).thenReturn(bindingResult);

        var response = handler.handleValidationErrors(validationException);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Erro de validação nos campos enviados.");
        assertThat(response.getBody().errors())
                .containsEntry("items", "O pedido deve conter pelo menos um item.")
                .containsEntry("items[0].quantity", "A quantidade deve ser no mínimo 1.");
    }
}
