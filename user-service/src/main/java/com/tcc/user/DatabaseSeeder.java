package com.tcc.user;

import com.tcc.user.entity.UserRole;
import com.tcc.user.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseSeeder {

    @Bean
    public CommandLineRunner seedUsers(UserService userService) {
        return args -> {
            userService.seedIfAbsent("Administrador", "admin@tcc.com", "admin123", UserRole.ADMIN);
            userService.seedIfAbsent("João Silva", "joao@email.com", "cliente123", UserRole.CUSTOMER);
        };
    }
}
