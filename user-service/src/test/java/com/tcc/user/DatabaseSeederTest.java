package com.tcc.user;

import com.tcc.user.entity.UserRole;
import com.tcc.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Testes unitários do DatabaseSeeder (JUnit 5 + Mockito).
 *
 * O seeder é o único ponto que cria os usuários de demonstração citados no README
 * (admin@tcc.com / joao@email.com). Ele delega ao UserService — nunca ao
 * repositório — para manter o acesso a dados restrito à camada de serviço, e essa
 * delegação é justamente o que se verifica aqui, sem subir contexto Spring.
 */
@ExtendWith(MockitoExtension.class)
class DatabaseSeederTest {

    @Mock UserService userService;

    private final DatabaseSeeder seeder = new DatabaseSeeder();

    @Test
    @DisplayName("Deve semear o ADMIN e o CUSTOMER padrão, delegando ao UserService")
    void seedUsers_ShouldSeedAdminAndCustomer() throws Exception {
        var runner = seeder.seedUsers(userService);
        assertThat(runner).isNotNull();

        runner.run();

        var order = inOrder(userService);
        order.verify(userService).seedIfAbsent("Administrador", "admin@tcc.com", "admin123", UserRole.ADMIN);
        order.verify(userService).seedIfAbsent("João Silva", "joao@email.com", "cliente123", UserRole.CUSTOMER);
        verifyNoMoreInteractions(userService);
    }

    @Test
    @DisplayName("Não deve semear nada antes do CommandLineRunner ser executado")
    void seedUsers_ShouldNotSeedBeforeRun() {
        seeder.seedUsers(userService);

        verifyNoMoreInteractions(userService);
    }
}
