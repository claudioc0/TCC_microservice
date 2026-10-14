package com.tcc.product.exception;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do GlobalExceptionHandler (JUnit 5 + Mockito).
 *
 * O handler é exercitado diretamente, sem contexto Spring: o contrato testado é
 * "exceção de domínio → status HTTP + corpo de erro".
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Mock MethodArgumentNotValidException validationException;

    @Test
    @DisplayName("ProductNotFoundException deve virar 404 com a mensagem da exceção")
    void handleProductNotFound_ShouldReturnNotFound() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleProductNotFound(new ProductNotFoundException(99L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().message()).isEqualTo("Produto não encontrado com id: 99");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("InsufficientStockException deve virar 422 detalhando solicitado x disponível")
    void handleInsufficientStock_ShouldReturnUnprocessableEntity() {
        var response = handler.handleInsufficientStock(new InsufficientStockException(1L, 10, 3));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(422);
        assertThat(response.getBody().message()).contains("Solicitado: 10", "Disponível: 3");
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
    @DisplayName("Erros de validação devem virar 400 com o mapa campo -> mensagem")
    void handleValidationErrors_ShouldReturnBadRequestWithFieldMap() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "productRequest");
        bindingResult.addError(new FieldError("productRequest", "name", "O nome do produto é obrigatório."));
        bindingResult.addError(new FieldError("productRequest", "price", "O preço deve ser maior que zero."));
        when(validationException.getBindingResult()).thenReturn(bindingResult);

        var response = handler.handleValidationErrors(validationException);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Erro de validação nos campos enviados.");
        assertThat(response.getBody().errors())
                .containsEntry("name", "O nome do produto é obrigatório.")
                .containsEntry("price", "O preço deve ser maior que zero.");
    }
}
