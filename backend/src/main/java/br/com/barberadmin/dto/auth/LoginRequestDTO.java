package br.com.barberadmin.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados para autenticação e login no sistema")
public class LoginRequestDTO {

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Schema(description = "E-mail cadastrado do profissional", example = "contato@barberman.com.br")
    private String email;

    @NotBlank(message = "A senha é obrigatória")
    @Schema(description = "Senha do usuário", example = "admin123")
    private String password;
}
