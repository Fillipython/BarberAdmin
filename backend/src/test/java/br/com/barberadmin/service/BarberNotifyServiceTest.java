package br.com.barberadmin.service;

import br.com.barberadmin.domain.entity.*;
import br.com.barberadmin.domain.enums.AppointmentStatus;
import br.com.barberadmin.domain.enums.UserRole;
import br.com.barberadmin.dto.barbernotify.BarberNotifyEventDTO;
import br.com.barberadmin.dto.barbernotify.BarberNotifyResponseDTO;
import br.com.barberadmin.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BarberNotifyServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private BarberNotifyService barberNotifyService;

    private User barber;
    private ServiceEntity service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(barberNotifyService, "autoConfirm", true);

        barber = User.builder()
                .id(UUID.randomUUID())
                .name("Lucas Santana")
                .role(UserRole.BARBER)
                .commissionPercentage(new BigDecimal("50.00"))
                .isActive(true)
                .build();

        service = ServiceEntity.builder()
                .id(UUID.randomUUID())
                .name("Corte Degradê")
                .priceCents(4500)
                .durationMinutes(30)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Deve processar webhook do BarberNotify criando cliente novo e agendamento confirmado")
    void testProcessIncomingEventNewClient() {
        BarberNotifyEventDTO event = BarberNotifyEventDTO.builder()
                .eventType("APPOINTMENT_CREATED")
                .clientName("Gabriel Menezes")
                .clientPhone("11999998877")
                .clientEmail("gabriel@email.com")
                .barberName("Lucas Santana")
                .serviceName("Corte Degradê")
                .scheduledAt(OffsetDateTime.now().plusDays(1))
                .durationMinutes(30)
                .customPriceCents(4500)
                .origin("WHATSAPP")
                .build();

        // Cliente não existe ainda
        when(clientRepository.findFirstByPhone("11999998877")).thenReturn(Optional.empty());
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> {
            Client c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        // Barbeiro e Serviço encontrados
        when(userRepository.findAllActiveStaff()).thenReturn(List.of(barber));
        when(serviceRepository.findFirstByNameIgnoreCaseAndIsActiveTrue("Corte Degradê")).thenReturn(Optional.of(service));

        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment a = invocation.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        BarberNotifyResponseDTO response = barberNotifyService.processIncomingEvent(event);

        assertTrue(response.isSuccess());
        assertNotNull(response.getAppointmentId());
        assertNotNull(response.getClientId());
        assertEquals(barber.getId(), response.getBarberId());

        verify(clientRepository, times(1)).save(any(Client.class));
        verify(appointmentRepository, times(1)).save(argThat(app -> {
            assertEquals(AppointmentStatus.CONFIRMED, app.getStatus());
            assertEquals(4500, app.getFinalPriceCents());
            assertEquals("Origem: BarberNotify", app.getNotes());
            return true;
        }));
    }

    @Test
    @DisplayName("Deve processar evento de cancelamento com sucesso")
    void testProcessCancellationEvent() {
        BarberNotifyEventDTO event = BarberNotifyEventDTO.builder()
                .eventType("APPOINTMENT_CANCELLED")
                .clientName("Gabriel Menezes")
                .clientPhone("11999998877")
                .build();

        BarberNotifyResponseDTO response = barberNotifyService.processIncomingEvent(event);

        assertTrue(response.isSuccess());
        assertEquals("Cancelamento registrado com sucesso", response.getMessage());
        verify(appointmentRepository, never()).save(any());
    }
}
