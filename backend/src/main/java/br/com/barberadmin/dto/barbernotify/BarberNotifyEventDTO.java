package br.com.barberadmin.dto.barbernotify;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO para recebimento de eventos e agendamentos enviados pelo BarberNotify
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarberNotifyEventDTO {

    @NotBlank(message = "O tipo do evento é obrigatório (ex: APPOINTMENT_CREATED, APPOINTMENT_CANCELLED)")
    private String eventType;

    private String externalId;

    @NotBlank(message = "O nome do cliente é obrigatório")
    private String clientName;

    @NotBlank(message = "O telefone do cliente é obrigatório")
    private String clientPhone;

    private String clientEmail;

    private String barberName;

    private String barberId;

    @NotBlank(message = "O serviço solicitado é obrigatório")
    private String serviceName;

    @NotNull(message = "A data e hora do agendamento são obrigatórias")
    private OffsetDateTime scheduledAt;

    private Integer durationMinutes;

    private Integer customPriceCents;

    private String notes;

    private String origin; // ex: "WHATSAPP", "BOT", "PORTAL"
}
