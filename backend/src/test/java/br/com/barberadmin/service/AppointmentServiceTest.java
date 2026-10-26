package br.com.barberadmin.service;

import br.com.barberadmin.domain.entity.*;
import br.com.barberadmin.domain.enums.*;
import br.com.barberadmin.dto.appointment.*;
import br.com.barberadmin.exception.BusinessException;
import br.com.barberadmin.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private FinancialRecordRepository financialRecordRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    private User barberMaster;
    private User regularBarber;
    private Client client;
    private ServiceEntity serviceEntity;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        barberMaster = User.builder()
                .id(UUID.randomUUID())
                .name("Alexandre Duarte")
                .role(UserRole.MASTER)
                .commissionPercentage(new BigDecimal("100.00"))
                .isActive(true)
                .build();

        regularBarber = User.builder()
                .id(UUID.randomUUID())
                .name("Lucas Santana")
                .role(UserRole.BARBER)
                .commissionPercentage(new BigDecimal("50.00"))
                .isActive(true)
                .build();

        client = Client.builder()
                .id(UUID.randomUUID())
                .name("Rodrigo Albuquerque")
                .phone("11977771002")
                .totalVisits(5)
                .totalSpentCents(25000L)
                .build();

        serviceEntity = ServiceEntity.builder()
                .id(UUID.randomUUID())
                .name("Corte Tradicional")
                .priceCents(4500)
                .durationMinutes(30)
                .build();

        appointment = Appointment.builder()
                .id(UUID.randomUUID())
                .client(client)
                .barber(regularBarber)
                .status(AppointmentStatus.CONFIRMED)
                .totalPriceCents(5000)
                .finalPriceCents(5000)
                .scheduledAt(OffsetDateTime.now())
                .endAt(OffsetDateTime.now().plusMinutes(30))
                .services(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Deve realizar checkout calculando comissão de 50% para o barbeiro e gravando registro financeiro")
    void testCheckoutWithCommissionCalculation() {
        UUID appointmentId = appointment.getId();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(financialRecordRepository.save(any(FinancialRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutPaymentDTO checkoutDTO = CheckoutPaymentDTO.builder()
                .paymentMethod(PaymentMethod.PIX)
                .finalAmountCents(6000) // R$ 60,00
                .notes("Pago via chave PIX da barbearia")
                .build();

        AppointmentResponseDTO response = appointmentService.checkout(appointmentId, checkoutDTO);

        assertNotNull(response);
        assertEquals(AppointmentStatus.COMPLETED, appointment.getStatus());
        assertEquals(6000, appointment.getFinalPriceCents());

        // Verifica gravação em financial_records com comissão calculada (50% de 6000 = 3000)
        verify(financialRecordRepository, times(1)).save(argThat(record -> {
            assertEquals(6000, record.getAmountCents());
            assertEquals(3000, record.getBarberCommissionCents());
            assertEquals(3000, record.getShopNetCents());
            assertEquals(PaymentMethod.PIX, record.getPaymentMethod());
            return true;
        }));

        // Verifica atualização das métricas do cliente
        assertEquals(6, client.getTotalVisits());
        assertEquals(31000L, client.getTotalSpentCents());
        verify(clientRepository, times(1)).save(client);
    }

    @Test
    @DisplayName("Deve permitir override de preço quando o agendamento ainda não estiver concluído")
    void testOverridePriceSuccess() {
        UUID appointmentId = appointment.getId();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PriceOverrideDTO overrideDTO = PriceOverrideDTO.builder()
                .priceCents(4000) // Preço promocional
                .reason("Desconto especial cliente recorrente")
                .build();

        AppointmentResponseDTO response = appointmentService.overridePrice(appointmentId, overrideDTO);

        assertNotNull(response);
        assertEquals(4000, appointment.getFinalPriceCents());
        assertEquals(4000, appointment.getTotalPriceCents());
        verify(appointmentRepository, times(1)).save(appointment);
    }

    @Test
    @DisplayName("Deve lançar BusinessException ao tentar alterar preço de agendamento já COMPLETED")
    void testOverridePriceFailsWhenAlreadyCompleted() {
        appointment.setStatus(AppointmentStatus.COMPLETED);
        UUID appointmentId = appointment.getId();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        PriceOverrideDTO overrideDTO = PriceOverrideDTO.builder().priceCents(3000).build();

        assertThrows(BusinessException.class, () -> appointmentService.overridePrice(appointmentId, overrideDTO));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve permitir troca de serviço e atualizar valores no agendamento")
    void testChangeServiceSuccess() {
        UUID appointmentId = appointment.getId();
        UUID newServiceId = UUID.randomUUID();
        ServiceEntity comboService = ServiceEntity.builder()
                .id(newServiceId)
                .name("Corte & Barba")
                .priceCents(5500)
                .durationMinutes(60)
                .build();

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(serviceRepository.findById(newServiceId)).thenReturn(Optional.of(comboService));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceChangeDTO changeDTO = ServiceChangeDTO.builder()
                .serviceId(newServiceId)
                .customPriceCents(5000)
                .build();

        AppointmentResponseDTO response = appointmentService.changeService(appointmentId, changeDTO);

        assertNotNull(response);
        assertEquals(5000, appointment.getFinalPriceCents());
        assertEquals(1, appointment.getServices().size());
        assertEquals("Corte & Barba", appointment.getServices().getFirst().getServiceName());
    }
}
