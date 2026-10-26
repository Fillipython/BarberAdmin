package br.com.barberadmin.service;

import br.com.barberadmin.domain.entity.*;
import br.com.barberadmin.domain.enums.*;
import br.com.barberadmin.dto.dashboard.DashboardDailyDTO;
import br.com.barberadmin.dto.dashboard.DonutSliceDTO;
import br.com.barberadmin.repository.FinancialRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private FinancialRecordRepository financialRecordRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("Deve consolidar rendimento diário e gerar distribuição de fatias de pagamento")
    void testGetDailyDashboardCalculations() {
        LocalDate date = LocalDate.of(2026, 10, 25);

        User master = User.builder().id(UUID.randomUUID()).name("Alexandre Duarte").build();
        User barber = User.builder().id(UUID.randomUUID()).name("Lucas Santana").build();
        Client client = Client.builder().id(UUID.randomUUID()).name("Cliente Teste").build();

        FinancialRecord rec1 = FinancialRecord.builder()
                .id(UUID.randomUUID())
                .amountCents(5000)
                .barberCommissionCents(2500)
                .shopNetCents(2500)
                .paymentMethod(PaymentMethod.PIX)
                .barber(barber)
                .client(client)
                .paidAt(OffsetDateTime.now())
                .build();

        FinancialRecord rec2 = FinancialRecord.builder()
                .id(UUID.randomUUID())
                .amountCents(5000)
                .barberCommissionCents(5000)
                .shopNetCents(0)
                .paymentMethod(PaymentMethod.DINHEIRO)
                .barber(master)
                .client(client)
                .paidAt(OffsetDateTime.now())
                .build();

        when(financialRecordRepository.findAllInTimeRange(any(), any())).thenReturn(List.of(rec1, rec2));

        DashboardDailyDTO daily = dashboardService.getDailyDashboard(date, null, UserRole.MASTER);

        assertNotNull(daily);
        assertEquals(10000L, daily.getGrossCents()); // R$ 100,00 total bruto
        assertEquals(7500L, daily.getBarberCommissionCents()); // R$ 75,00 comissões
        assertEquals(2500L, daily.getShopNetCents()); // R$ 25,00 líquido barbearia
        assertEquals(2, daily.getTotalCompletedAppointments());

        // Gráfico Donut de Formas de Pagamento
        List<DonutSliceDTO> paymentDonut = daily.getPaymentMethodsDonut();
        assertEquals(2, paymentDonut.size());

        DonutSliceDTO pixSlice = paymentDonut.stream().filter(s -> s.getLabel().equals("PIX")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("50.0"), pixSlice.getPercentage());
        assertEquals("#10b981", pixSlice.getColor());
    }
}
