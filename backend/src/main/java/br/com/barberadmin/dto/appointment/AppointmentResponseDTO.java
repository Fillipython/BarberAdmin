package br.com.barberadmin.dto.appointment;

import br.com.barberadmin.domain.enums.AppointmentStatus;
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
public class AppointmentResponseDTO {

    private UUID id;
    private UUID clientId;
    private String clientName;
    private String clientPhone;

    private UUID barberId;
    private String barberName;
    private String barberNickname;

    private UUID serviceId;
    private String serviceName;

    private OffsetDateTime scheduledAt;
    private OffsetDateTime endAt;
    private String timeFormatted; // ex: "09:00"

    private AppointmentStatus status;
    private Integer priceCents;
    private String priceFormatted; // ex: "R$ 45,00"

    private PaymentMethod paymentMethod;
    private Integer barberCommissionCents;
    private Integer shopNetCents;

    private String notes;
}
