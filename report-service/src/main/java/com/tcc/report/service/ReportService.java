package com.tcc.report.service;

import com.tcc.report.client.OrderClient;
import com.tcc.report.client.dto.OrderSummaryDto;
import com.tcc.report.dto.SalesReportResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class ReportService {

    private static final String ENTREGUE = "ENTREGUE";
    private static final String CANCELADO = "CANCELADO";

    private final OrderClient orderClient;

    public ReportService(OrderClient orderClient) {
        this.orderClient = orderClient;
    }

    /**
     * Gera o relatório de vendas para o período [startDate, endDate], agregando
     * pedidos obtidos via HTTP no order-service.
     */
    public SalesReportResponse generateReport(LocalDate startDate, LocalDate endDate, String bearerToken) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("A data de início não pode ser posterior à data de fim.");
        }

        List<OrderSummaryDto> orders = orderClient.findByPeriod(startDate, endDate, bearerToken);

        long totalOrders = orders.size();
        long deliveredOrders = orders.stream().filter(o -> ENTREGUE.equals(o.status())).count();
        long canceledOrders = orders.stream().filter(o -> CANCELADO.equals(o.status())).count();

        BigDecimal totalRevenue = orders.stream()
                .filter(o -> !CANCELADO.equals(o.status()))
                .map(OrderSummaryDto::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SalesReportResponse(startDate, endDate, totalOrders, deliveredOrders, canceledOrders,
                totalRevenue, orders);
    }
}
