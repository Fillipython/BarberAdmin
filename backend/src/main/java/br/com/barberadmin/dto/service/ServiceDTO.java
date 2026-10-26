package br.com.barberadmin.dto.service;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class ServiceDTO {

    private UUID id;

    @NotBlank(message = "O nome do serviço é obrigatório")
    private String name;

    private String description;
    private String category;

    @NotNull(message = "O preço é obrigatório")
    @Min(value = 0, message = "O preço não pode ser negativo")
    private Integer priceCents;

    private String priceFormatted;

    @NotNull(message = "A duração em minutos é obrigatória")
    @Min(value = 5, message = "A duração mínima é de 5 minutos")
    private Integer durationMinutes;

    private Boolean isActive;
    private Integer displayOrder;
}
