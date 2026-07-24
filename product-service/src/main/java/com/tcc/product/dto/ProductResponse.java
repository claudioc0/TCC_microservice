package com.tcc.product.dto;

import com.tcc.product.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(Long id, String name, String description, BigDecimal price,
                               Integer stockQuantity, LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                p.getStockQuantity(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
