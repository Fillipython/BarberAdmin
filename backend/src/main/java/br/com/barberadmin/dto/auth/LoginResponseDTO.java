package br.com.barberadmin.dto.auth;

import br.com.barberadmin.domain.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Resposta do login bem-sucedido com dados do usuário e token de sessão")
public class LoginResponseDTO {

    private UUID id;
    private String name;
    private String nickname;
    private String email;
    private String phone;
    private UserRole role;
    private BigDecimal commissionPercentage;
    private String avatarUrl;
    private String token;
    private String message;
}
