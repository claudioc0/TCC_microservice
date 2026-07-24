package com.tcc.order.client.dto;

import java.math.BigDecimal;

/**
 * Espelho LOCAL do contrato exposto pelo product-service (subconjunto de campos
 * que o order-service precisa). Nunca é a mesma classe do outro serviço — não há
 * JAR/classpath compartilhado entre eles.
 */
public record ProductDto(Long id, String name, BigDecimal price, Integer stockQuantity) {}
