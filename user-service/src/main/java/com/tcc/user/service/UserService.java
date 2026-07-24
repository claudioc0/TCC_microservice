package com.tcc.user.service;

import com.tcc.user.dto.AuthResponse;
import com.tcc.user.dto.LoginRequest;
import com.tcc.user.dto.RegisterRequest;
import com.tcc.user.dto.UserResponse;
import com.tcc.user.entity.User;
import com.tcc.user.entity.UserRole;
import com.tcc.user.exception.DuplicateEmailException;
import com.tcc.user.exception.InvalidCredentialsException;
import com.tcc.user.exception.UserNotFoundException;
import com.tcc.user.repository.UserRepository;
import com.tcc.user.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        User user = new User(request.name(), request.email(),
                passwordEncoder.encode(request.password()), UserRole.CUSTOMER);
        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved.getId(), saved.getEmail(), saved.getRole().name());

        return AuthResponse.of(token, saved.getId(), saved.getName(), saved.getEmail(), saved.getRole().name());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return AuthResponse.of(token, user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }

    /** Usado apenas pelo DatabaseSeeder, mantendo o acesso ao repositório restrito à camada de serviço. */
    public void seedIfAbsent(String name, String email, String rawPassword, UserRole role) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        userRepository.save(new User(name, email, passwordEncoder.encode(rawPassword), role));
    }
}
