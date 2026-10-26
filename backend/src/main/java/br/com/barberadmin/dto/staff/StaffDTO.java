package br.com.barberadmin.dto.staff;

import br.com.barberadmin.domain.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffDTO {

    private UUID id;

    @NotBlank(message = "O nome é obrigatório")
    private String name;

    private String nickname;

    @NotBlank(message = "O email é obrigatório")
    private String email;

    @NotBlank(message = "O telefone é obrigatório")
    private String phone;

    private UserRole role;
    private BigDecimal commissionPercentage;
    private String avatarUrl;
    private String bio;
    private Boolean isActive;
}
