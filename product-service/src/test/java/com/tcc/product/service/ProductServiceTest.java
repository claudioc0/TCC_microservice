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
import java.util.List;
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

    @Test
    @DisplayName("delete deve remover produto existente")
    void deleteShouldRemoveExisting() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        productService.delete(1L);

        verify(productRepository).deleteById(1L);
    }

    @Test
    @DisplayName("findById deve retornar o produto mapeado quando existe")
    void findByIdShouldReturnMappedProductWhenFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        var result = productService.findById(1L);

        assertThat(result.name()).isEqualTo("Monitor");
        assertThat(result.stockQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("findAll deve retornar a lista mapeada de produtos")
    void findAllShouldReturnMappedList() {
        when(productRepository.findAll()).thenReturn(List.of(existing));

        var result = productService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Monitor");
    }

    @Test
    @DisplayName("searchByName deve retornar a lista mapeada filtrada por nome")
    void searchByNameShouldReturnMappedList() {
        when(productRepository.findByNameContainingIgnoreCase("Mon")).thenReturn(List.of(existing));

        var result = productService.searchByName("Mon");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Monitor");
    }

    @Test
    @DisplayName("update deve modificar nome, descrição, preço e estoque, e salvar")
    void updateShouldModifyAllFieldsAndSave() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new ProductRequest("Monitor 4K", "32 pol", new BigDecimal("1500.00"), 3);
        var result = productService.update(1L, request);

        assertThat(result.name()).isEqualTo("Monitor 4K");
        assertThat(result.description()).isEqualTo("32 pol");
        assertThat(result.price()).isEqualByComparingTo("1500.00");
        assertThat(result.stockQuantity()).isEqualTo(3);
        verify(productRepository).save(existing);
    }

    @Test
    @DisplayName("update deve lançar exceção para id inexistente")
    void updateShouldThrowWhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        var request = new ProductRequest("X", "Y", new BigDecimal("1.00"), 1);
        assertThatThrownBy(() -> productService.update(99L, request)).isInstanceOf(ProductNotFoundException.class);
        verify(productRepository, never()).save(any());
    }
}
