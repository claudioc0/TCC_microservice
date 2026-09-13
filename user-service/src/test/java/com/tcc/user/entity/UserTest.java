package com.tcc.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    @DisplayName("Construtor deve preencher nome, e-mail, senha e role")
    void constructorShouldPopulateAllFields() {
        User user = new User("Maria", "maria@test.com", "hashed-pass", UserRole.ADMIN);

        assertThat(user.getName()).isEqualTo("Maria");
        assertThat(user.getEmail()).isEqualTo("maria@test.com");
        assertThat(user.getPassword()).isEqualTo("hashed-pass");
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @DisplayName("getId deve retornar o id atribuído (simulando persistência)")
    void getIdShouldReturnAssignedId() {
        User user = new User("Maria", "maria@test.com", "hashed-pass", UserRole.CUSTOMER);
        ReflectionTestUtils.setField(user, "id", 3L);

        assertThat(user.getId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("onCreate (@PrePersist) deve preencher createdAt")
    void onCreateShouldSetCreatedAt() {
        User user = new User("Maria", "maria@test.com", "hashed-pass", UserRole.CUSTOMER);

        user.onCreate();

        assertThat(user.getCreatedAt()).isNotNull();
    }
}
