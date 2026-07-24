package com.tcc.order.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(Long productId) {
        super("Estoque insuficiente para o produto id=" + productId);
    }
}
