package com.tcc.report.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Espelho LOCAL do contrato exposto pelo order-service (subconjunto de campos
 * necessário para agregação). Não compartilha classe/JAR com o order-service.
 */
public record OrderSummaryDto(Long id, Long userId, BigDecimal totalAmount, String status, LocalDateTime createdAt) {}
