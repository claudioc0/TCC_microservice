package com.tcc.order.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long productId) {
        super("Produto não encontrado no product-service com id: " + productId);
    }
}
