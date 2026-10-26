package br.com.barberadmin.dto.client;

import jakarta.validation.constraints.NotBlank;
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
public class ClientDTO {

    private UUID id;

    @NotBlank(message = "O nome do cliente é obrigatório")
    private String name;

    @NotBlank(message = "O telefone é obrigatório")
    private String phone;

    private String email;
    private String notes;
    private Integer totalVisits;
    private Long totalSpentCents;
    private String totalSpentFormatted;
    private OffsetDateTime lastVisitAt;
}
