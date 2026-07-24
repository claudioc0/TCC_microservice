package com.tcc.order.controller;

import com.tcc.order.dto.OrderRequest;
import com.tcc.order.dto.OrderResponse;
import com.tcc.order.dto.UpdateStatusRequest;
import com.tcc.order.security.AuthenticatedPrincipal;
import com.tcc.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Cria um pedido para o usuário autenticado. O userId vem do JWT (nunca do body,
     * evitando fraude de impersonação); o token bruto é repassado ao product-service.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @AuthenticationPrincipal AuthenticatedPrincipal currentUser,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.create(currentUser.id(), request, authorizationHeader));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponse>> myOrders(@AuthenticationPrincipal AuthenticatedPrincipal currentUser) {
        return ResponseEntity.ok(orderService.findByUser(currentUser.id()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.findById(id));
    }

    /**
     * Lista todos os pedidos — somente ADMIN.
     * Se startDate/endDate forem informados, filtra por período (usado pelo report-service).
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> findAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(orderService.findByPeriod(startDate, endDate));
        }
        return ResponseEntity.ok(orderService.findAll());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(orderService.updateStatus(id, request));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancel(@PathVariable Long id,
                                                 @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        return ResponseEntity.ok(orderService.cancel(id, authorizationHeader));
    }
}
