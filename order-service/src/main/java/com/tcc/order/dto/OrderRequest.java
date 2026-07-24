package com.tcc.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderRequest(
    @NotEmpty(message = "O pedido deve conter pelo menos um item.")
    @Valid
    List<OrderItemRequest> items
) {
    public record OrderItemRequest(
        @NotNull(message = "O ID do produto é obrigatório.") Long productId,
        @NotNull(message = "A quantidade é obrigatória.")
        @Min(value = 1, message = "A quantidade deve ser no mínimo 1.") Integer quantity
    ) {}
}
