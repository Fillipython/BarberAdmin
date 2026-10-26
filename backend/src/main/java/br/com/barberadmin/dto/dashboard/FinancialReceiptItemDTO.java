package br.com.barberadmin.dto.dashboard;

import br.com.barberadmin.domain.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialReceiptItemDTO {

    private UUID id;
    private String clientName;
    private String barberName;
    private PaymentMethod paymentMethod;
    private Long amountCents;
    private String amountFormatted;
    private Long barberCommissionCents;
    private Long shopNetCents;
    private OffsetDateTime paidAt;
    private String timeFormatted;
}
