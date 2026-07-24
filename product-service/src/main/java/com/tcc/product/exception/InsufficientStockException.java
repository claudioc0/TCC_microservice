package com.tcc.product.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(Long productId, int requested, int available) {
        super(String.format(
            "Estoque insuficiente para o produto id=%d. Solicitado: %d, Disponível: %d.",
            productId, requested, available));
    }
}
