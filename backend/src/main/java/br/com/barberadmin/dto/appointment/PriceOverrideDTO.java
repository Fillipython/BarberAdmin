package br.com.barberadmin.dto.appointment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceOverrideDTO {

    @NotNull(message = "O novo preço é obrigatório")
    @Min(value = 0, message = "O preço não pode ser negativo")
    private Integer priceCents;

    private String reason;
}
