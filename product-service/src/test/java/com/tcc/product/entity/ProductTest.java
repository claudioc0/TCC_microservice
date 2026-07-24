package com.tcc.product.entity;

import com.tcc.product.exception.InsufficientStockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class ProductTest {

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
