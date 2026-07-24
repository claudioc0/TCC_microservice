package com.tcc.order.service;

import com.tcc.order.client.ProductClient;
import com.tcc.order.client.dto.ProductDto;
import com.tcc.order.dto.OrderRequest;
import com.tcc.order.dto.OrderRequest.OrderItemRequest;
import com.tcc.order.dto.UpdateStatusRequest;
import com.tcc.order.entity.Order;
import com.tcc.order.entity.OrderItem;
import com.tcc.order.entity.OrderStatus;
import com.tcc.order.exception.InvalidStatusTransitionException;
import com.tcc.order.exception.OrderNotFoundException;
import com.tcc.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock ProductClient productClient;
    @InjectMocks OrderService orderService;

    private static final String TOKEN = "Bearer test-token";

    @Test
    @DisplayName("create deve consultar e descontar estoque via ProductClient")
    void createShouldQueryAndDecreaseStockViaClient() {
        when(productClient.getProduct(1L, TOKEN)).thenReturn(new ProductDto(1L, "Mouse", new BigDecimal("150.00"), 5));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new OrderRequest(List.of(new OrderItemRequest(1L, 2)));
        var response = orderService.create(1L, request, TOKEN);

        assertThat(response.status()).isEqualTo(OrderStatus.PENDENTE);
        assertThat(response.totalAmount()).isEqualByComparingTo("300.00");
        verify(productClient).decreaseStock(1L, 2, TOKEN);
    }

    @Test
    @DisplayName("updateStatus deve seguir o fluxo PENDENTE → CONFIRMADO")
    void updateStatusShouldTransitionCorrectly() {
        Order order = new Order(1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = orderService.updateStatus(1L, new UpdateStatusRequest(OrderStatus.CONFIRMADO));

        assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMADO);
    }

    @Test
    @DisplayName("updateStatus deve lançar exceção para transição inválida ENTREGUE → PENDENTE")
    void updateStatusShouldThrowForInvalidTransition() {
        Order order = new Order(1L);
        order.transitionTo(OrderStatus.CONFIRMADO);
        order.transitionTo(OrderStatus.ENVIADO);
        order.transitionTo(OrderStatus.ENTREGUE);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(1L, new UpdateStatusRequest(OrderStatus.PENDENTE)))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("cancel deve cancelar pedido PENDENTE e devolver estoque via ProductClient")
    void cancelShouldCancelAndRestoreStockViaClient() {
        Order order = new Order(1L);
        order.addItem(new OrderItem(order, 5L, "Teclado", new BigDecimal("300.00"), 2));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = orderService.cancel(1L, TOKEN);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELADO);
        verify(productClient).increaseStock(5L, 2, TOKEN);
    }

    @Test
    @DisplayName("findById deve lançar OrderNotFoundException para id inexistente")
    void findByIdShouldThrowForUnknownId() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(99L)).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("findByUser deve retornar pedidos do usuário")
    void findByUserShouldReturnUserOrders() {
        when(orderRepository.findByUserId(1L)).thenReturn(List.of(new Order(1L)));

        assertThat(orderService.findByUser(1L)).hasSize(1);
    }

    @Test
    @DisplayName("findByPeriod deve delegar ao repositório com o intervalo do dia completo")
    void findByPeriodShouldQueryFullDayRange() {
        var start = java.time.LocalDate.of(2024, 1, 1);
        var end = java.time.LocalDate.of(2024, 1, 31);
        when(orderRepository.findByCreatedAtBetween(any(), any())).thenReturn(List.of(new Order(1L)));

        assertThat(orderService.findByPeriod(start, end)).hasSize(1);
        verify(orderRepository).findByCreatedAtBetween(start.atStartOfDay(), end.atTime(23, 59, 59, 999999999));
    }
}
