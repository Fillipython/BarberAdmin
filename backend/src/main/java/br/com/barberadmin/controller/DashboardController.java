package br.com.barberadmin.controller;

import br.com.barberadmin.domain.enums.UserRole;
import br.com.barberadmin.dto.dashboard.DashboardDailyDTO;
import br.com.barberadmin.dto.dashboard.DashboardMonthlyDTO;
import br.com.barberadmin.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard Financeiro", description = "Rendimento Diário e Mensal com gráficos de pizza/donut e extratos")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Obter Rendimento Diário", description = "Retorna o rendimento financeiro da data, fatias dos gráficos donut (pagamento e barbeiros) e lista de recebimentos")
    @GetMapping("/daily")
    public ResponseEntity<DashboardDailyDTO> getDailyDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID barberId,
            @RequestParam(required = false, defaultValue = "MASTER") UserRole role) {

        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        DashboardDailyDTO response = dashboardService.getDailyDashboard(targetDate, barberId, role);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obter Rendimento Mensal", description = "Retorna o consolidado mensal com gráficos de distribuição")
    @GetMapping("/monthly")
    public ResponseEntity<DashboardMonthlyDTO> getMonthlyDashboard(
            @RequestParam(required = false) String month, // Formato "YYYY-MM" ex: "2026-10"
            @RequestParam(required = false) UUID barberId,
            @RequestParam(required = false, defaultValue = "MASTER") UserRole role) {

        YearMonth targetMonth = (month != null && !month.isBlank())
                ? YearMonth.parse(month)
                : YearMonth.now();

        DashboardMonthlyDTO response = dashboardService.getMonthlyDashboard(targetMonth, barberId, role);
        return ResponseEntity.ok(response);
    }
}
