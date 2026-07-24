package com.tcc.order.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Guarda o snapshot do produto no momento da compra (preco-no-momento-da-compra),
 * garantindo que alteracoes futuras de preco no product-service nao afetem pedidos antigos.
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** ID do produto no product-service — nunca um @ManyToOne: os servicos não compartilham banco. */
    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    protected OrderItem() {}

    public OrderItem(Order order, Long productId, String productName, BigDecimal unitPrice, Integer quantity) {
        this.order = order;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public Integer getQuantity() { return quantity; }
}
