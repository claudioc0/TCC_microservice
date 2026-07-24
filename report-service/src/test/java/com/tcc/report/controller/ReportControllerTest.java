package com.tcc.report.controller;

import com.tcc.report.dto.SalesReportResponse;
import com.tcc.report.security.JwtAuthenticationFilter;
import com.tcc.report.security.JwtService;
import com.tcc.report.security.SecurityConfig;
import com.tcc.report.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes do ReportController.
 *
 * Foco: isolar a camada de Controller, validar o tratamento de requisições HTTP
 * e a restrição de acesso a usuários ADMIN.
 *
 * SecurityConfig é importada explicitamente para que a regra hasRole("ADMIN") em
 * /api/reports/** seja realmente exercitada neste slice de teste.
 */
@WebMvcTest(ReportController.class)
@Import(SecurityConfig.class)
class ReportControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean ReportService reportService;

    // Necessário para o contexto de segurança: o JwtAuthenticationFilter (um @Component
    // Filter) é carregado automaticamente pelo @WebMvcTest e depende de JwtService.
    @MockBean JwtService jwtService;

    @Test
    @DisplayName("Deve retornar 200 OK e o relatório de vendas quando as datas são válidas")
    @WithMockUser(roles = "ADMIN")
    void getSalesReport_WithValidDates_ShouldReturnOkAndReport() throws Exception {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        var mockResponse = new SalesReportResponse(
                startDate, endDate, 10L, 8L, 1L, new BigDecimal("5000.00"), Collections.emptyList());

        when(reportService.generateReport(startDate, endDate, "Bearer admin-token")).thenReturn(mockResponse);

        mockMvc.perform(get("/api/reports/sales")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders", is(10)))
                .andExpect(jsonPath("$.totalRevenue", is(5000.00)))
                .andExpect(jsonPath("$.startDate", is("2024-01-01")));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o serviço lança IllegalArgumentException")
    @WithMockUser(roles = "ADMIN")
    void getSalesReport_WhenServiceThrowsException_ShouldReturnBadRequest() throws Exception {
        LocalDate startDate = LocalDate.of(2024, 2, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        when(reportService.generateReport(startDate, endDate, "Bearer admin-token"))
                .thenThrow(new IllegalArgumentException("A data de início não pode ser posterior à data de fim."));

        mockMvc.perform(get("/api/reports/sales")
                        .param("startDate", "2024-02-01")
                        .param("endDate", "2024-01-31")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden quando usuário autenticado não tem role ADMIN")
    @WithMockUser(roles = "CUSTOMER")
    void getSalesReport_WithoutAdminRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/reports/sales")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .header("Authorization", "Bearer customer-token"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(reportService);
    }
}
