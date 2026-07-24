package com.tcc.order.service;

import com.tcc.order.client.ProductClient;
import com.tcc.order.client.dto.ProductDto;
import com.tcc.order.dto.OrderRequest;
import com.tcc.order.dto.OrderResponse;
import com.tcc.order.dto.UpdateStatusRequest;
import com.tcc.order.entity.Order;
import com.tcc.order.entity.OrderItem;
import com.tcc.order.entity.OrderStatus;
import com.tcc.order.exception.OrderNotFoundException;
import com.tcc.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;

    public OrderService(OrderRepository orderRepository, ProductClient productClient) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
    }

    // ── Criação ───────────────────────────────────────────────

    /**
     * Cria um pedido consultando e descontando estoque via HTTP no product-service.
     * O token do usuário original é repassado em cada chamada.
     */
    public OrderResponse create(Long userId, OrderRequest request, String bearerToken) {
        Order order = new Order(userId);

        for (OrderRequest.OrderItemRequest itemReq : request.items()) {
            ProductDto product = productClient.getProduct(itemReq.productId(), bearerToken);
            productClient.decreaseStock(itemReq.productId(), itemReq.quantity(), bearerToken);

            OrderItem item = new OrderItem(order, product.id(), product.name(), product.price(), itemReq.quantity());
            order.addItem(item);
        }

        return OrderResponse.from(orderRepository.save(order));
    }

    // ── Consultas ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return OrderResponse.from(getOrderOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findByUser(Long userId) {
        return orderRepository.findByUserId(userId).stream().map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return orderRepository.findAll().stream().map(OrderResponse::from).toList();
    }

    /** Usado pelo report-service para agregar vendas por período. */
    @Transactional(readOnly = true)
    public List<OrderResponse> findByPeriod(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        return orderRepository.findByCreatedAtBetween(start, end).stream().map(OrderResponse::from).toList();
    }

    // ── Atualização de status ─────────────────────────────────

    public OrderResponse updateStatus(Long orderId, UpdateStatusRequest request) {
        Order order = getOrderOrThrow(orderId);
        order.transitionTo(request.newStatus());
        return OrderResponse.from(orderRepository.save(order));
    }

    // ── Cancelamento ──────────────────────────────────────────

    /** Cancela o pedido e devolve o estoque de cada item via HTTP ao product-service. */
    public OrderResponse cancel(Long orderId, String bearerToken) {
        Order order = getOrderOrThrow(orderId);
        order.transitionTo(OrderStatus.CANCELADO);

        for (OrderItem item : order.getItems()) {
            productClient.increaseStock(item.getProductId(), item.getQuantity(), bearerToken);
        }

        return OrderResponse.from(orderRepository.save(order));
    }

    private Order getOrderOrThrow(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }
}
