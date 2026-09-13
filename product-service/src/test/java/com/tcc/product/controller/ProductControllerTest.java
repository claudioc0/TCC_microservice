package com.tcc.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.product.dto.ProductRequest;
import com.tcc.product.dto.ProductResponse;
import com.tcc.product.dto.StockAdjustmentRequest;
import com.tcc.product.exception.InsufficientStockException;
import com.tcc.product.exception.ProductNotFoundException;
import com.tcc.product.security.JwtService;
import com.tcc.product.security.SecurityConfig;
import com.tcc.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ProductService productService;
    @MockBean JwtService jwtService;

    private static final LocalDateTime NOW = LocalDateTime.of(2024, 1, 1, 10, 0);

    @Test
    @DisplayName("GET /api/products deve retornar 200 e a lista, sem autenticação")
    void findAll_ShouldReturnOkWithList() throws Exception {
        var product = new ProductResponse(1L, "Mouse", "Gamer", new BigDecimal("150.00"), 10, NOW, NOW);
        when(productService.findAll()).thenReturn(List.of(product));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("Mouse")))
                .andExpect(jsonPath("$[0].stockQuantity", is(10)));
    }

    @Test
    @DisplayName("GET /api/products/{id} deve retornar 200 e o produto")
    void findById_ShouldReturnOk() throws Exception {
        var product = new ProductResponse(1L, "Mouse", "Gamer", new BigDecimal("150.00"), 10, NOW, NOW);
        when(productService.findById(1L)).thenReturn(product);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Mouse")))
                .andExpect(jsonPath("$.price", is(150.00)));
    }

    @Test
    @DisplayName("GET /api/products/{id} deve retornar 404 quando produto não existe")
    void findById_ShouldReturnNotFoundWhenMissing() throws Exception {
        when(productService.findById(99L)).thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/products/search deve retornar produtos filtrados por nome")
    void searchByName_ShouldReturnFilteredList() throws Exception {
        var product = new ProductResponse(2L, "Teclado Mecânico", "desc", new BigDecimal("300.00"), 5, NOW, NOW);
        when(productService.searchByName("Teclado")).thenReturn(List.of(product));

        mockMvc.perform(get("/api/products/search").param("name", "Teclado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Teclado Mecânico")));
    }

    @Test
    @DisplayName("POST /api/products com role ADMIN deve retornar 201 e criar o produto")
    @WithMockUser(roles = "ADMIN")
    void create_WithAdminRole_ShouldReturnCreated() throws Exception {
        var request = new ProductRequest("Monitor", "27 pol", new BigDecimal("900.00"), 8);
        var response = new ProductResponse(3L, "Monitor", "27 pol", new BigDecimal("900.00"), 8, NOW, NOW);
        when(productService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.name", is("Monitor")));

        verify(productService, times(1)).create(request);
    }

    @Test
    @DisplayName("POST /api/products sem role ADMIN deve retornar 403")
    @WithMockUser(roles = "CUSTOMER")
    void create_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
        var request = new ProductRequest("Monitor", "27 pol", new BigDecimal("900.00"), 8);

        mockMvc.perform(post("/api/products")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("POST /api/products sem autenticação deve ser rejeitado")
    void create_WithoutAuthentication_ShouldBeRejected() throws Exception {
        var request = new ProductRequest("Monitor", "27 pol", new BigDecimal("900.00"), 8);

        mockMvc.perform(post("/api/products")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());

        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("POST /api/products com corpo inválido deve retornar 400")
    @WithMockUser(roles = "ADMIN")
    void create_WithInvalidBody_ShouldReturnBadRequest() throws Exception {
        var invalidRequest = new ProductRequest("", "desc", new BigDecimal("-1.00"), -5);

        mockMvc.perform(post("/api/products")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("PUT /api/products/{id} com role ADMIN deve atualizar e retornar 200")
    @WithMockUser(roles = "ADMIN")
    void update_WithAdminRole_ShouldReturnOk() throws Exception {
        var request = new ProductRequest("Monitor 4K", "27 pol", new BigDecimal("1200.00"), 6);
        var response = new ProductResponse(3L, "Monitor 4K", "27 pol", new BigDecimal("1200.00"), 6, NOW, NOW);
        when(productService.update(3L, request)).thenReturn(response);

        mockMvc.perform(put("/api/products/3")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Monitor 4K")));
    }

    @Test
    @DisplayName("DELETE /api/products/{id} com role ADMIN deve retornar 204")
    @WithMockUser(roles = "ADMIN")
    void delete_WithAdminRole_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/products/3"))
                .andExpect(status().isNoContent());

        verify(productService, times(1)).delete(3L);
    }

    @Test
    @DisplayName("DELETE /api/products/{id} sem role ADMIN deve retornar 403")
    @WithMockUser(roles = "CUSTOMER")
    void delete_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(delete("/api/products/3"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("PATCH .../stock/decrease autenticado deve retornar 200 e o estoque atualizado")
    @WithMockUser(roles = "CUSTOMER")
    void decreaseStock_Authenticated_ShouldReturnOk() throws Exception {
        var response = new ProductResponse(1L, "Mouse", "Gamer", new BigDecimal("150.00"), 8, NOW, NOW);
        when(productService.decreaseStock(1L, 2)).thenReturn(response);

        mockMvc.perform(patch("/api/products/1/stock/decrease")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new StockAdjustmentRequest(2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity", is(8)));
    }

    @Test
    @DisplayName("PATCH .../stock/decrease deve retornar 422 quando estoque é insuficiente")
    @WithMockUser(roles = "CUSTOMER")
    void decreaseStock_WhenInsufficientStock_ShouldReturnUnprocessableEntity() throws Exception {
        when(productService.decreaseStock(1L, 100)).thenThrow(new InsufficientStockException(1L, 100, 5));

        mockMvc.perform(patch("/api/products/1/stock/decrease")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new StockAdjustmentRequest(100))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("PATCH .../stock/increase autenticado deve retornar 200")
    @WithMockUser(roles = "CUSTOMER")
    void increaseStock_Authenticated_ShouldReturnOk() throws Exception {
        var response = new ProductResponse(1L, "Mouse", "Gamer", new BigDecimal("150.00"), 15, NOW, NOW);
        when(productService.increaseStock(1L, 5)).thenReturn(response);

        mockMvc.perform(patch("/api/products/1/stock/increase")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new StockAdjustmentRequest(5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity", is(15)));
    }
}
