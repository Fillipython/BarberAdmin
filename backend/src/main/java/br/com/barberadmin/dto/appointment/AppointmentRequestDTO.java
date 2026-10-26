package br.com.barberadmin.dto.appointment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRequestDTO {

    @NotNull(message = "O cliente é obrigatório")
    private UUID clientId;

    @NotNull(message = "O barbeiro é obrigatório")
    private UUID barberId;

    @NotNull(message = "O horário do agendamento é obrigatório")
    private OffsetDateTime scheduledAt;

    private Integer durationMinutes;

    @NotNull(message = "Selecione pelo menos um serviço")
    private List<UUID> serviceIds;

    private Integer customPriceCents;

    private String notes;
}
