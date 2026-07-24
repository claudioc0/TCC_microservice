package com.tcc.order.exception;

/** Lançada quando a chamada HTTP ao product-service falha (indisponível ou erro inesperado). */
public class ProductServiceUnavailableException extends RuntimeException {
    public ProductServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
