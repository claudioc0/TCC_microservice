package com.tcc.report.service;

import com.tcc.report.client.OrderClient;
import com.tcc.report.client.dto.OrderSummaryDto;
import com.tcc.report.dto.SalesReportResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock OrderClient orderClient;
    @InjectMocks ReportService reportService;

    private static final String TOKEN = "Bearer admin-token";

    @Test
    @DisplayName("Deve lançar exceção quando data de início é posterior à data de fim")
    void shouldThrowWhenStartDateAfterEndDate() {
        LocalDate start = LocalDate.of(2024, 2, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);

        assertThatThrownBy(() -> reportService.generateReport(start, end, TOKEN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A data de início não pode ser posterior à data de fim.");

        verifyNoInteractions(orderClient);
    }

    @Test
    @DisplayName("Deve aceitar data de início igual à data de fim")
    void shouldAcceptEqualStartAndEndDate() {
        when(orderClient.findByPeriod(any(), any(), any())).thenReturn(List.of());
        LocalDate today = LocalDate.now();

        assertThatNoException().isThrownBy(() -> reportService.generateReport(today, today, TOKEN));
    }

    @Test
    @DisplayName("Deve calcular totais corretamente a partir dos pedidos retornados pelo order-service")
    void shouldComputeTotalsFromOrderClientResponse() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 10, 0);

        var delivered = new OrderSummaryDto(1L, 10L, new BigDecimal("300.00"), "ENTREGUE", now);
        var canceled = new OrderSummaryDto(2L, 11L, new BigDecimal("50.00"), "CANCELADO", now);
        var pending = new OrderSummaryDto(3L, 12L, new BigDecimal("120.00"), "PENDENTE", now);

        when(orderClient.findByPeriod(start, end, TOKEN)).thenReturn(List.of(delivered, canceled, pending));

        SalesReportResponse report = reportService.generateReport(start, end, TOKEN);

        assertThat(report.totalOrders()).isEqualTo(3L);
        assertThat(report.deliveredOrders()).isEqualTo(1L);
        assertThat(report.canceledOrders()).isEqualTo(1L);
        assertThat(report.totalRevenue()).isEqualByComparingTo("420.00"); // 300 + 120 (exclui cancelado)
        assertThat(report.orders()).hasSize(3);
        assertThat(report.startDate()).isEqualTo(start);
        assertThat(report.endDate()).isEqualTo(end);
    }

    @Test
    @DisplayName("Deve retornar totais zerados quando não há pedidos no período")
    void shouldReturnZeroedTotalsWhenNoOrders() {
        when(orderClient.findByPeriod(any(), any(), any())).thenReturn(List.of());

        LocalDate today = LocalDate.now();
        SalesReportResponse report = reportService.generateReport(today, today, TOKEN);

        assertThat(report.totalOrders()).isZero();
        assertThat(report.totalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Deve repassar o token do usuário original ao OrderClient")
    void shouldForwardBearerTokenToOrderClient() {
        LocalDate today = LocalDate.now();
        when(orderClient.findByPeriod(today, today, TOKEN)).thenReturn(List.of());

        reportService.generateReport(today, today, TOKEN);

        verify(orderClient).findByPeriod(today, today, TOKEN);
    }
}
