package com.tcc.order.entity;

import com.tcc.order.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

class OrderStatusTransitionTest {

    private Order newOrder() { return new Order(1L); }

    // ── Transições válidas ────────────────────────────────────

    @Test
    @DisplayName("PENDENTE → CONFIRMADO deve ser permitido")
    void pendenteToConfirmadoAllowed() {
        Order order = newOrder();
        assertThatNoException().isThrownBy(() -> order.transitionTo(OrderStatus.CONFIRMADO));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMADO);
    }

    @Test
    @DisplayName("CONFIRMADO → ENVIADO deve ser permitido")
    void confirmadoToEnviadoAllowed() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.CONFIRMADO);
        assertThatNoException().isThrownBy(() -> order.transitionTo(OrderStatus.ENVIADO));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ENVIADO);
    }

    @Test
    @DisplayName("ENVIADO → ENTREGUE deve ser permitido")
    void enviadoToEntregueAllowed() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.CONFIRMADO);
        order.transitionTo(OrderStatus.ENVIADO);
        assertThatNoException().isThrownBy(() -> order.transitionTo(OrderStatus.ENTREGUE));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ENTREGUE);
    }

    @Test
    @DisplayName("PENDENTE → CANCELADO deve ser permitido")
    void pendenteToCanceladoAllowed() {
        Order order = newOrder();
        assertThatNoException().isThrownBy(() -> order.transitionTo(OrderStatus.CANCELADO));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELADO);
    }

    @Test
    @DisplayName("CONFIRMADO → CANCELADO deve ser permitido")
    void confirmadoToCanceladoAllowed() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.CONFIRMADO);
        assertThatNoException().isThrownBy(() -> order.transitionTo(OrderStatus.CANCELADO));
    }

    // ── Transições inválidas ──────────────────────────────────

    @Test
    @DisplayName("PENDENTE → ENVIADO deve ser bloqueado (pula etapa)")
    void pendenteToEnviadoBlocked() {
        Order order = newOrder();
        assertThatThrownBy(() -> order.transitionTo(OrderStatus.ENVIADO))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("PENDENTE")
                .hasMessageContaining("ENVIADO");
    }

    @Test
    @DisplayName("PENDENTE → ENTREGUE deve ser bloqueado (pula etapa)")
    void pendenteToEntregueBlocked() {
        Order order = newOrder();
        assertThatThrownBy(() -> order.transitionTo(OrderStatus.ENTREGUE))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("CONFIRMADO → PENDENTE deve ser bloqueado (não retrocede)")
    void confirmadoToPendenteBlocked() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.CONFIRMADO);
        assertThatThrownBy(() -> order.transitionTo(OrderStatus.PENDENTE))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("CONFIRMADO → ENTREGUE deve ser bloqueado (pula etapa)")
    void confirmadoToEntregueBlocked() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.CONFIRMADO);
        assertThatThrownBy(() -> order.transitionTo(OrderStatus.ENTREGUE))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("ENVIADO → CANCELADO deve ser bloqueado")
    void enviadoToCanceladoBlocked() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.CONFIRMADO);
        order.transitionTo(OrderStatus.ENVIADO);
        assertThatThrownBy(() -> order.transitionTo(OrderStatus.CANCELADO))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("ENTREGUE → qualquer status deve ser bloqueado (estado terminal)")
    void entregueIsTerminal() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.CONFIRMADO);
        order.transitionTo(OrderStatus.ENVIADO);
        order.transitionTo(OrderStatus.ENTREGUE);

        for (OrderStatus s : OrderStatus.values()) {
            assertThatThrownBy(() -> order.transitionTo(s)).isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    @Test
    @DisplayName("CANCELADO → qualquer status deve ser bloqueado (estado terminal)")
    void canceladoIsTerminal() {
        Order order = newOrder();
        order.transitionTo(OrderStatus.CANCELADO);

        for (OrderStatus s : OrderStatus.values()) {
            assertThatThrownBy(() -> order.transitionTo(s)).isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    @Test
    @DisplayName("totalAmount deve ser zero para pedido sem itens")
    void totalAmountShouldBeZeroForEmptyOrder() {
        Order order = newOrder();
        assertThat(order.getTotalAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("addItem deve recalcular o total")
    void addItemShouldRecalculateTotal() {
        Order order = newOrder();
        order.addItem(new OrderItem(order, 1L, "Mouse", new java.math.BigDecimal("150.00"), 2));
        assertThat(order.getTotalAmount()).isEqualByComparingTo("300.00");
    }

    // ── Getters / timestamps (Order) ────────────────────────────

    @Test
    @DisplayName("Construtor deve preencher userId e status inicial PENDENTE")
    void constructorShouldPopulateUserIdAndInitialStatus() {
        Order order = new Order(42L);

        assertThat(order.getUserId()).isEqualTo(42L);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDENTE);
    }

    @Test
    @DisplayName("getId deve retornar o id atribuído (simulando persistência)")
    void getIdShouldReturnAssignedId() {
        Order order = newOrder();
        ReflectionTestUtils.setField(order, "id", 9L);

        assertThat(order.getId()).isEqualTo(9L);
    }

    @Test
    @DisplayName("onCreate (@PrePersist) deve preencher createdAt e updatedAt")
    void onCreateShouldSetTimestamps() {
        Order order = newOrder();

        order.onCreate();

        assertThat(order.getCreatedAt()).isNotNull();
        assertThat(order.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("onUpdate (@PreUpdate) deve atualizar apenas updatedAt")
    void onUpdateShouldRefreshOnlyUpdatedAt() throws InterruptedException {
        Order order = newOrder();
        order.onCreate();
        var createdAt = order.getCreatedAt();
        Thread.sleep(5);

        order.onUpdate();

        assertThat(order.getCreatedAt()).isEqualTo(createdAt);
        assertThat(order.getUpdatedAt()).isAfter(createdAt);
    }

    // ── Getters (OrderItem) ──────────────────────────────────────

    @Test
    @DisplayName("OrderItem deve expor pedido, produto, preço e quantidade")
    void orderItemGettersShouldReturnConstructorValues() {
        Order order = newOrder();
        OrderItem item = new OrderItem(order, 7L, "Teclado", new java.math.BigDecimal("250.00"), 3);
        ReflectionTestUtils.setField(item, "id", 15L);

        assertThat(item.getId()).isEqualTo(15L);
        assertThat(item.getOrder()).isSameAs(order);
        assertThat(item.getProductId()).isEqualTo(7L);
        assertThat(item.getProductName()).isEqualTo("Teclado");
        assertThat(item.getUnitPrice()).isEqualByComparingTo("250.00");
        assertThat(item.getQuantity()).isEqualTo(3);
    }
}
