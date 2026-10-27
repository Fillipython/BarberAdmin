package br.com.barberadmin.controller;

import br.com.barberadmin.dto.auth.LoginRequestDTO;
import br.com.barberadmin.dto.auth.LoginResponseDTO;
import br.com.barberadmin.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Autenticação de profissionais e controle de sessão do BarberAdmin")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Realizar Login", description = "Valida e-mail e senha (BCrypt) e retorna o perfil do usuário autenticado")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        LoginResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
