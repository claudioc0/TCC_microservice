package com.tcc.report.dto;

import com.tcc.report.client.dto.OrderSummaryDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SalesReportResponse(
    LocalDate startDate, LocalDate endDate, long totalOrders,
    long deliveredOrders, long canceledOrders, BigDecimal totalRevenue,
    List<OrderSummaryDto> orders
) {}
