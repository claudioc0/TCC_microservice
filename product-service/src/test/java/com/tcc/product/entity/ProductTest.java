package com.tcc.product.entity;

import com.tcc.product.exception.InsufficientStockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class ProductTest {

    @Test
    @DisplayName("Construtor deve preencher nome, descrição, preço e estoque")
    void constructorShouldPopulateAllFields() {
        Product p = new Product("Mouse", "Gamer", new BigDecimal("150.00"), 10);

        assertThat(p.getName()).isEqualTo("Mouse");
        assertThat(p.getDescription()).isEqualTo("Gamer");
        assertThat(p.getPrice()).isEqualByComparingTo("150.00");
        assertThat(p.getStockQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("getId deve retornar o id atribuído (simulando persistência)")
    void getIdShouldReturnAssignedId() {
        Product p = new Product("Mouse", "Gamer", new BigDecimal("150.00"), 10);
        ReflectionTestUtils.setField(p, "id", 7L);

        assertThat(p.getId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("onCreate (@PrePersist) deve preencher createdAt e updatedAt")
    void onCreateShouldSetTimestamps() {
        Product p = new Product("Mouse", "Gamer", new BigDecimal("150.00"), 10);

        p.onCreate();

        assertThat(p.getCreatedAt()).isNotNull();
        assertThat(p.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("onUpdate (@PreUpdate) deve atualizar apenas updatedAt")
    void onUpdateShouldRefreshOnlyUpdatedAt() throws InterruptedException {
        Product p = new Product("Mouse", "Gamer", new BigDecimal("150.00"), 10);
        p.onCreate();
        var createdAt = p.getCreatedAt();
        Thread.sleep(5);

        p.onUpdate();

        assertThat(p.getCreatedAt()).isEqualTo(createdAt);
        assertThat(p.getUpdatedAt()).isAfter(createdAt);
    }

    @Test
    @DisplayName("setName, setDescription, setPrice e setStockQuantity devem alterar os campos")
    void settersShouldModifyFields() {
        Product p = new Product("Mouse", "Gamer", new BigDecimal("150.00"), 10);

        p.setName("Mouse Pro");
        p.setDescription("Gamer RGB");
        p.setPrice(new BigDecimal("199.90"));
        p.setStockQuantity(20);

        assertThat(p.getName()).isEqualTo("Mouse Pro");
        assertThat(p.getDescription()).isEqualTo("Gamer RGB");
        assertThat(p.getPrice()).isEqualByComparingTo("199.90");
        assertThat(p.getStockQuantity()).isEqualTo(20);
    }

    @Test
    @DisplayName("Deve reduzir estoque corretamente")
    void shouldDecreaseStock() {
        Product p = new Product("Mouse", "Gamer", new BigDecimal("150.00"), 10);
        p.decreaseStock(3);
        assertThat(p.getStockQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("Deve lançar InsufficientStockException quando solicitado maior que disponível")
    void shouldThrowWhenInsufficientStock() {
        Product p = new Product("Teclado", "Mecânico", new BigDecimal("120.00"), 2);
        assertThatThrownBy(() -> p.decreaseStock(5)).isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("Deve permitir reduzir estoque até exatamente zero")
    void shouldAllowDecreaseToExactZero() {
        Product p = new Product("Item", "desc", new BigDecimal("10.00"), 5);
        p.decreaseStock(5);
        assertThat(p.getStockQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("Deve aumentar estoque corretamente")
    void shouldIncreaseStock() {
        Product p = new Product("Item", "desc", new BigDecimal("10.00"), 5);
        p.increaseStock(3);
        assertThat(p.getStockQuantity()).isEqualTo(8);
    }
}
