package br.com.barberadmin.dto.barbernotify;

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
public class BarberNotifyResponseDTO {

    private boolean success;
    private String message;
    private UUID appointmentId;
    private UUID clientId;
    private UUID barberId;
    private OffsetDateTime processedAt;
}
