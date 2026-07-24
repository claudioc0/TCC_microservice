package com.tcc.order.dto;

import com.tcc.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
    @NotNull(message = "O novo status é obrigatório.") OrderStatus newStatus
) {}
