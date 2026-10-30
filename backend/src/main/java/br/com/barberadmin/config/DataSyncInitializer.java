package br.com.barberadmin.config;

import br.com.barberadmin.domain.entity.*;
import br.com.barberadmin.domain.enums.*;
import br.com.barberadmin.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class DataSyncInitializer {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final ServiceRepository serviceRepository;
    private final AppointmentRepository appointmentRepository;
    private final FinancialRecordRepository financialRecordRepository;

    private static final ZoneId ZONE_BR = ZoneId.of("America/Sao_Paulo");

    private static final Map<UUID, String> BARBERNOTIFY_DEFAULT_AVATARS = Map.of(
            UUID.fromString("8a350074-5524-453d-a625-2b25e7e8ca59"),
            "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=250&auto=format&fit=crop&q=80",
            UUID.fromString("70b1fee3-a338-4444-90f9-f4551afc15fe"),
            "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=250&auto=format&fit=crop&q=80",
            UUID.fromString("0670b734-45c1-4435-ad0e-b8d6b3099c7e"),
            "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=250&auto=format&fit=crop&q=80"
    );

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void syncInitialDataWithBarberNotify() {
        try {
            // 1. Garante que os 3 barbeiros sincronizados com o BarberNotify tenham suas fotos padrão se ainda estiverem nulas
            for (Map.Entry<UUID, String> entry : BARBERNOTIFY_DEFAULT_AVATARS.entrySet()) {
                userRepository.findById(entry.getKey()).ifPresent(user -> {
                    if (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) {
                        user.setAvatarUrl(entry.getValue());
                        userRepository.save(user);
                        log.info("Foto padrão do BarberNotify sincronizada para o barbeiro {}", user.getName());
                    }
                });
            }

            // 2. Garante mock interligado de agendamentos para o dia atual caso a agenda de hoje esteja vazia
            LocalDate today = LocalDate.now(ZONE_BR);
            OffsetDateTime startOfDay = today.atStartOfDay(ZONE_BR).toOffsetDateTime();
            OffsetDateTime endOfDay = today.plusDays(1).atStartOfDay(ZONE_BR).toOffsetDateTime();

            List<Appointment> todaysAppointments = appointmentRepository.findAllInTimeRange(startOfDay, endOfDay);
            if (todaysAppointments.isEmpty()) {
                seedInterlinkedMockForToday(today);
            }
        } catch (Exception e) {
            log.warn("Aviso ao sincronizar dados iniciais com BarberNotify: {}", e.getMessage());
        }
    }

    private void seedInterlinkedMockForToday(LocalDate today) {
        Optional<User> carlosOpt = userRepository.findById(UUID.fromString("8a350074-5524-453d-a625-2b25e7e8ca59"));
        Optional<User> lucasOpt = userRepository.findById(UUID.fromString("70b1fee3-a338-4444-90f9-f4551afc15fe"));
        Optional<User> matheusOpt = userRepository.findById(UUID.fromString("0670b734-45c1-4435-ad0e-b8d6b3099c7e"));

        List<Client> clients = clientRepository.findAll();
        List<ServiceEntity> services = serviceRepository.findAllByIsActiveTrueOrderByDisplayOrderAscNameAsc();
        if (carlosOpt.isEmpty() || clients.isEmpty() || services.isEmpty()) return;

        Client c1 = clients.get(0);
        Client c2 = clients.size() > 1 ? clients.get(1) : c1;
        Client c3 = clients.size() > 2 ? clients.get(2) : c1;
        Client c4 = clients.size() > 3 ? clients.get(3) : c1;

        ServiceEntity sPlatinado = services.stream().filter(s -> s.getName().toLowerCase().contains("platinado")).findFirst().orElse(services.get(0));
        ServiceEntity sCorteBarba = services.stream().filter(s -> s.getName().toLowerCase().contains("corte & barba")).findFirst().orElse(services.get(0));
        ServiceEntity sLuzes = services.stream().filter(s -> s.getName().toLowerCase().contains("luzes")).findFirst().orElse(services.get(0));
        ServiceEntity sCorte = services.stream().filter(s -> s.getName().equalsIgnoreCase("Corte")).findFirst().orElse(services.get(0));

        // 1. Agendamento Concluído hoje às 09:30 (Carlos Silva)
        createMockAppointment(carlosOpt.get(), c4, sCorte, today, LocalTime.of(9, 30), AppointmentStatus.COMPLETED, true);

        // 2. Agendamento Confirmado hoje às 11:00 (Carlos Silva - interligado com BarberNotify)
        createMockAppointment(carlosOpt.get(), c1, sPlatinado, today, LocalTime.of(11, 0), AppointmentStatus.CONFIRMED, false);

        // 3. Agendamento Confirmado hoje às 14:00 (Lucas Santana - interligado com BarberNotify)
        lucasOpt.ifPresent(lucas -> createMockAppointment(lucas, c2, sCorteBarba, today, LocalTime.of(14, 0), AppointmentStatus.CONFIRMED, false));

        // 4. Agendamento Confirmado hoje às 16:00 (Matheus Oliveira - interligado com BarberNotify)
        matheusOpt.ifPresent(matheus -> createMockAppointment(matheus, c3, sLuzes, today, LocalTime.of(16, 0), AppointmentStatus.CONFIRMED, false));

        log.info("Mock interligado BarberNotify <-> BarberAdmin criado para a data {}", today);
    }

    private void createMockAppointment(User barber, Client client, ServiceEntity service, LocalDate date, LocalTime time, AppointmentStatus status, boolean createFinancial) {
        OffsetDateTime scheduledAt = date.atTime(time).atZone(ZONE_BR).toOffsetDateTime();
        if (appointmentRepository.existsActiveSlotConflict(barber.getId(), scheduledAt, null)) {
            return;
        }
        int duration = service.getDurationMinutes() != null ? service.getDurationMinutes() : 30;
        int price = service.getPriceCents() != null ? service.getPriceCents() : 3500;

        Appointment app = Appointment.builder()
                .client(client)
                .barber(barber)
                .scheduledAt(scheduledAt)
                .endAt(scheduledAt.plusMinutes(duration))
                .status(status)
                .totalPriceCents(price)
                .finalPriceCents(price)
                .discountCents(0)
                .notes("Origem: BarberNotify (Sincronizado)")
                .completedAt(status == AppointmentStatus.COMPLETED ? scheduledAt.plusMinutes(duration) : null)
                .build();

        AppointmentServiceItem item = AppointmentServiceItem.builder()
                .appointment(app)
                .service(service)
                .serviceName(service.getName())
                .priceCentsAtBooking(price)
                .durationMinutesAtBooking(duration)
                .build();
        app.addService(item);

        Appointment saved = appointmentRepository.save(app);

        if (createFinancial) {
            int comm = barber.getRole() == UserRole.MASTER ? price : price / 2;
            FinancialRecord record = FinancialRecord.builder()
                    .appointment(saved)
                    .barber(barber)
                    .client(client)
                    .type(TransactionType.RECEITA_SERVICO)
                    .amountCents(price)
                    .barberCommissionCents(comm)
                    .shopNetCents(price - comm)
                    .paymentMethod(PaymentMethod.PIX)
                    .paymentStatus(PaymentStatus.PAGO)
                    .paidAt(scheduledAt.plusMinutes(duration))
                    .description("Corte concluído - " + service.getName())
                    .build();
            financialRecordRepository.save(record);
        }
    }
}

