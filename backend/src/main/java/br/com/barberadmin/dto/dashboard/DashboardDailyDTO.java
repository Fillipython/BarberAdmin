package br.com.barberadmin.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDailyDTO {

    private LocalDate date;
    private String dateFormatted;

    // Totais financeiros
    private Long grossCents;
    private String grossFormatted;

    private Long barberCommissionCents;
    private String barberCommissionFormatted;

    private Long shopNetCents;
    private String shopNetFormatted;

    private Integer totalCompletedAppointments;

    // Gráficos Donut
    private List<DonutSliceDTO> paymentMethodsDonut;
    private List<DonutSliceDTO> barbersDonut;

    // Lista de recebimentos
    private List<FinancialReceiptItemDTO> receipts;
}
