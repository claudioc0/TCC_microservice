package com.tcc.order.dto;

import com.tcc.order.entity.Order;
import com.tcc.order.entity.OrderItem;
import com.tcc.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
    Long id, Long userId, List<OrderItemResponse> items, BigDecimal totalAmount,
    OrderStatus status, LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static OrderResponse from(Order o) {
        return new OrderResponse(
            o.getId(), o.getUserId(),
            o.getItems().stream().map(OrderItemResponse::from).toList(),
            o.getTotalAmount(), o.getStatus(), o.getCreatedAt(), o.getUpdatedAt());
    }

    public record OrderItemResponse(
        Long id, Long productId, String productName, BigDecimal unitPrice, Integer quantity, BigDecimal subtotal
    ) {
        public static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(item.getId(), item.getProductId(), item.getProductName(),
                    item.getUnitPrice(), item.getQuantity(), item.subtotal());
        }
    }
}
