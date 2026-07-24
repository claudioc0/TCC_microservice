package com.tcc.product.service;

import com.tcc.product.dto.ProductRequest;
import com.tcc.product.entity.Product;
import com.tcc.product.exception.InsufficientStockException;
import com.tcc.product.exception.ProductNotFoundException;
import com.tcc.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @InjectMocks ProductService productService;

    private Product existing;

    @BeforeEach
    void setUp() { existing = new Product("Monitor", "27 pol", new BigDecimal("800.00"), 10); }

    @Test
    @DisplayName("create deve salvar produto no repositório")
    void createShouldSave() {
        when(productRepository.save(any())).thenReturn(existing);

        var result = productService.create(new ProductRequest("Monitor", "27 pol", new BigDecimal("800.00"), 10));

        assertThat(result.name()).isEqualTo("Monitor");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("findById deve lançar exceção para id inexistente")
    void findByIdShouldThrowWhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L)).isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("decreaseStock deve delegar à entidade e salvar")
    void decreaseStockShouldDelegateAndSave() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any())).thenReturn(existing);

        var result = productService.decreaseStock(1L, 3);

        assertThat(result.stockQuantity()).isEqualTo(7);
        verify(productRepository).save(existing);
    }

    @Test
    @DisplayName("decreaseStock deve propagar exceção quando estoque insuficiente")
    void decreaseStockShouldPropagateWhenInsufficient() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> productService.decreaseStock(1L, 100))
                .isInstanceOf(InsufficientStockException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("increaseStock deve delegar à entidade e salvar")
    void increaseStockShouldDelegateAndSave() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any())).thenReturn(existing);

        var result = productService.increaseStock(1L, 5);

        assertThat(result.stockQuantity()).isEqualTo(15);
    }

    @Test
    @DisplayName("delete deve lançar exceção para id inexistente")
    void deleteShouldThrowWhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(99L)).isInstanceOf(ProductNotFoundException.class);
        verify(productRepository, never()).deleteById(any());
    }
}
