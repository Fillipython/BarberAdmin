package br.com.barberadmin.dto.appointment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceChangeDTO {

    @NotNull(message = "O ID do novo serviço é obrigatório")
    private UUID serviceId;

    private Integer customPriceCents;
}
