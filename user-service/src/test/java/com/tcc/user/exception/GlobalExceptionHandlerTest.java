package com.tcc.user.exception;

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
    @DisplayName("UserNotFoundException deve virar 404 com a mensagem da exceção")
    void handleUserNotFound_ShouldReturnNotFound() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleUserNotFound(new UserNotFoundException(99L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().message()).isEqualTo("Usuário não encontrado com id: 99");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("DuplicateEmailException deve virar 409 Conflict")
    void handleDuplicateEmail_ShouldReturnConflict() {
        var response = handler.handleDuplicateEmail(new DuplicateEmailException("maria@test.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).contains("maria@test.com");
    }

    @Test
    @DisplayName("InvalidCredentialsException deve virar 401 sem revelar se o e-mail existe")
    void handleInvalidCredentials_ShouldReturnUnauthorized() {
        var response = handler.handleInvalidCredentials(new InvalidCredentialsException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(401);
        assertThat(response.getBody().message()).isEqualTo("E-mail ou senha inválidos.");
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
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "registerRequest");
        bindingResult.addError(new FieldError("registerRequest", "email", "E-mail inválido."));
        bindingResult.addError(new FieldError("registerRequest", "password", "A senha deve ter no mínimo 6 caracteres."));
        when(validationException.getBindingResult()).thenReturn(bindingResult);

        var response = handler.handleValidationErrors(validationException);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Erro de validação nos campos enviados.");
        assertThat(response.getBody().errors())
                .containsEntry("email", "E-mail inválido.")
                .containsEntry("password", "A senha deve ter no mínimo 6 caracteres.");
    }
}
