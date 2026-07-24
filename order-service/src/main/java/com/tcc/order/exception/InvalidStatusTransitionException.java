package com.tcc.order.exception;

import com.tcc.order.entity.OrderStatus;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(Long orderId, OrderStatus current, OrderStatus next) {
        super(String.format(
            "Transição de status inválida para o pedido %d: %s → %s não é permitida.",
            orderId, current, next));
    }
}
