package br.com.barberadmin.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMonthlyDTO {

    private String yearMonth; // "2026-10"
    private String monthNameFormatted; // "Outubro de 2026"

    private Long grossCents;
    private String grossFormatted;

    private Long barberCommissionCents;
    private String barberCommissionFormatted;

    private Long shopNetCents;
    private String shopNetFormatted;

    private Integer totalCompletedAppointments;

    private List<DonutSliceDTO> paymentMethodsDonut;
    private List<DonutSliceDTO> barbersDonut;
}
