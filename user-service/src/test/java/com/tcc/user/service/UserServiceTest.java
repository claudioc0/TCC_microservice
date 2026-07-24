package com.tcc.user.service;

import com.tcc.user.dto.LoginRequest;
import com.tcc.user.dto.RegisterRequest;
import com.tcc.user.entity.User;
import com.tcc.user.entity.UserRole;
import com.tcc.user.exception.DuplicateEmailException;
import com.tcc.user.exception.InvalidCredentialsException;
import com.tcc.user.exception.UserNotFoundException;
import com.tcc.user.repository.UserRepository;
import com.tcc.user.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @InjectMocks UserService userService;

    private User existing;

    @BeforeEach
    void setUp() {
        existing = new User("Maria", "maria@test.com", "hashed-pass", UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("register deve salvar usuário com senha codificada e retornar token")
    void registerShouldSaveEncodedPasswordAndReturnToken() {
        when(userRepository.existsByEmail("maria@test.com")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hashed-pass");
        when(userRepository.save(any(User.class))).thenReturn(existing);
        when(jwtService.generateToken(null, "maria@test.com", "CUSTOMER")).thenReturn("token-abc");

        var response = userService.register(new RegisterRequest("Maria", "maria@test.com", "senha123"));

        assertThat(response.token()).isEqualTo("token-abc");
        assertThat(response.email()).isEqualTo("maria@test.com");
        assertThat(response.role()).isEqualTo("CUSTOMER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register deve lançar exceção quando e-mail já existe")
    void registerShouldThrowWhenEmailExists() {
        when(userRepository.existsByEmail("maria@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(new RegisterRequest("Maria", "maria@test.com", "senha123")))
                .isInstanceOf(DuplicateEmailException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("login deve retornar token quando credenciais são válidas")
    void loginShouldReturnTokenWhenValid() {
        when(userRepository.findByEmail("maria@test.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("senha123", "hashed-pass")).thenReturn(true);
        when(jwtService.generateToken(null, "maria@test.com", "CUSTOMER")).thenReturn("token-xyz");

        var response = userService.login(new LoginRequest("maria@test.com", "senha123"));

        assertThat(response.token()).isEqualTo("token-xyz");
    }

    @Test
    @DisplayName("login deve lançar exceção quando e-mail não existe")
    void loginShouldThrowWhenEmailNotFound() {
        when(userRepository.findByEmail("desconhecido@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(new LoginRequest("desconhecido@test.com", "qualquer")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("login deve lançar exceção quando senha está incorreta")
    void loginShouldThrowWhenPasswordWrong() {
        when(userRepository.findByEmail("maria@test.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("errada", "hashed-pass")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(new LoginRequest("maria@test.com", "errada")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("findById deve lançar exceção para id inexistente")
    void findByIdShouldThrowWhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("findAll deve retornar lista mapeada")
    void findAllShouldReturnMappedList() {
        when(userRepository.findAll()).thenReturn(List.of(existing));

        assertThat(userService.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("delete deve remover usuário existente")
    void deleteShouldRemoveExisting() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.delete(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("delete deve lançar exceção para id inexistente")
    void deleteShouldThrowWhenNotFound() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.delete(99L)).isInstanceOf(UserNotFoundException.class);
        verify(userRepository, never()).deleteById(any());
    }
}
