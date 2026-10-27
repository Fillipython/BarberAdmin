package br.com.barberadmin.service;

import br.com.barberadmin.domain.entity.User;
import br.com.barberadmin.domain.enums.UserRole;
import br.com.barberadmin.dto.auth.LoginRequestDTO;
import br.com.barberadmin.dto.auth.LoginResponseDTO;
import br.com.barberadmin.exception.UnauthorizedException;
import br.com.barberadmin.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private User masterUser;
    private final String rawPassword = "admin123";
    private final String encodedPassword = new BCryptPasswordEncoder().encode(rawPassword);

    @BeforeEach
    void setUp() {
        masterUser = User.builder()
                .id(UUID.randomUUID())
                .name("Carlos Silva")
                .nickname("Mestre Carlos")
                .email("contato@barberman.com.br")
                .phone("11988880001")
                .passwordHash(encodedPassword)
                .role(UserRole.MASTER)
                .commissionPercentage(new BigDecimal("100.00"))
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Deve realizar login com sucesso quando credenciais forem válidas")
    void testLoginSuccess() {
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("contato@barberman.com.br"))
                .thenReturn(Optional.of(masterUser));

        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("contato@barberman.com.br")
                .password(rawPassword)
                .build();

        LoginResponseDTO response = authService.login(request);

        assertNotNull(response);
        assertEquals(masterUser.getId(), response.getId());
        assertEquals("Carlos Silva", response.getName());
        assertEquals(UserRole.MASTER, response.getRole());
        assertNotNull(response.getToken());
        assertTrue(response.getToken().startsWith("barber_token_"));
    }

    @Test
    @DisplayName("Deve lançar UnauthorizedException quando a senha estiver incorreta")
    void testLoginWrongPassword() {
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("contato@barberman.com.br"))
                .thenReturn(Optional.of(masterUser));

        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("contato@barberman.com.br")
                .password("senha_errada")
                .build();

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().contains("E-mail ou senha incorretos"));
    }

    @Test
    @DisplayName("Deve lançar UnauthorizedException quando o e-mail não existir")
    void testLoginUserNotFound() {
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("inexistente@barber.com"))
                .thenReturn(Optional.empty());

        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("inexistente@barber.com")
                .password("qualquer_senha")
                .build();

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().contains("E-mail ou senha incorretos"));
    }

    @Test
    @DisplayName("Deve lançar UnauthorizedException quando o usuário estiver inativo")
    void testLoginUserInactive() {
        masterUser.setIsActive(false);
        when(userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("contato@barberman.com.br"))
                .thenReturn(Optional.of(masterUser));

        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("contato@barberman.com.br")
                .password(rawPassword)
                .build();

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().contains("inativa"));
    }
}
