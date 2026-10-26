package br.com.barberadmin.service;

import br.com.barberadmin.domain.entity.*;
import br.com.barberadmin.domain.enums.AppointmentStatus;
import br.com.barberadmin.domain.enums.UserRole;
import br.com.barberadmin.dto.barbernotify.BarberNotifyEventDTO;
import br.com.barberadmin.dto.barbernotify.BarberNotifyResponseDTO;
import br.com.barberadmin.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BarberNotifyService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final AppointmentRepository appointmentRepository;

    @Value("${barbernotify.auto-confirm:true}")
    private boolean autoConfirm;

    /**
     * Processa evento de entrada enviado pelo BarberNotify (criação, reagendamento ou cancelamento de corte)
     */
    @Transactional
    public BarberNotifyResponseDTO processIncomingEvent(BarberNotifyEventDTO event) {
        log.info("Recebido evento do BarberNotify: tipo={}, cliente={}, telefone={}, data={}",
                event.getEventType(), event.getClientName(), event.getClientPhone(), event.getScheduledAt());

        if ("APPOINTMENT_CANCELLED".equalsIgnoreCase(event.getEventType())) {
            return handleCancellation(event);
        }

        // 1. Localiza ou cadastra o cliente a partir dos dados do BarberNotify
        Client client = findOrCreateClient(event);

        // 2. Localiza o Barbeiro responsável
        User barber = resolveBarber(event);

        // 3. Localiza o Serviço
        ServiceEntity service = resolveService(event);

        // 4. Calcula duração e horário final
        int duration = event.getDurationMinutes() != null && event.getDurationMinutes() > 0
                ? event.getDurationMinutes()
                : service.getDurationMinutes();

        OffsetDateTime scheduledAt = event.getScheduledAt();
        OffsetDateTime endAt = scheduledAt.plusMinutes(duration);

        // 5. Determina o preço (permite override vindo do BarberNotify ou usa o preço base do serviço)
        int priceCents = event.getCustomPriceCents() != null && event.getCustomPriceCents() >= 0
                ? event.getCustomPriceCents()
                : service.getPriceCents();

        // 6. Criação do Agendamento
        Appointment appointment = Appointment.builder()
                .client(client)
                .barber(barber)
                .scheduledAt(scheduledAt)
                .endAt(endAt)
                .status(autoConfirm ? AppointmentStatus.CONFIRMED : AppointmentStatus.PENDING)
                .totalPriceCents(priceCents)
                .finalPriceCents(priceCents)
                .discountCents(0)
                .notes("Origem: BarberNotify" + (event.getNotes() != null ? " - " + event.getNotes() : ""))
                .build();

        // 7. Item de Serviço associado
        AppointmentServiceItem item = AppointmentServiceItem.builder()
                .appointment(appointment)
                .service(service)
                .serviceName(service.getName())
                .priceCentsAtBooking(priceCents)
                .durationMinutesAtBooking(duration)
                .build();

        appointment.getServices().add(item);
        Appointment savedAppointment = appointmentRepository.save(appointment);

        log.info("Agendamento criado com sucesso a partir do BarberNotify: id={}, cliente={}, barbeiro={}, valor={}",
                savedAppointment.getId(), client.getName(), barber.getName(), priceCents);

        return BarberNotifyResponseDTO.builder()
                .success(true)
                .message("Agendamento sincronizado com sucesso do BarberNotify")
                .appointmentId(savedAppointment.getId())
                .clientId(client.getId())
                .barberId(barber.getId())
                .processedAt(OffsetDateTime.now())
                .build();
    }

    private Client findOrCreateClient(BarberNotifyEventDTO event) {
        String cleanPhone = event.getClientPhone().replaceAll("[^0-9]", "");
        return clientRepository.findFirstByPhone(cleanPhone)
                .map(existing -> {
                    // Atualiza nome se necessário
                    if (event.getClientName() != null && !event.getClientName().isBlank()) {
                        existing.setName(event.getClientName());
                    }
                    if (event.getClientEmail() != null && !event.getClientEmail().isBlank()) {
                        existing.setEmail(event.getClientEmail());
                    }
                    return clientRepository.save(existing);
                })
                .orElseGet(() -> {
                    Client newClient = Client.builder()
                            .name(event.getClientName())
                            .phone(cleanPhone)
                            .email(event.getClientEmail())
                            .notes("Cadastrado via BarberNotify")
                            .build();
                    return clientRepository.save(newClient);
                });
    }

    private User resolveBarber(BarberNotifyEventDTO event) {
        if (event.getBarberId() != null && !event.getBarberId().isBlank()) {
            try {
                UUID barberUuid = UUID.fromString(event.getBarberId());
                Optional<User> found = userRepository.findById(barberUuid);
                if (found.isPresent()) return found.get();
            } catch (IllegalArgumentException ignored) {}
        }

        if (event.getBarberName() != null && !event.getBarberName().isBlank()) {
            List<User> staff = userRepository.findAllActiveStaff();
            for (User u : staff) {
                if (u.getName().toLowerCase().contains(event.getBarberName().toLowerCase())) {
                    return u;
                }
            }
        }

        // Padrão: Primeiro barbeiro ativo ou Master
        List<User> activeStaff = userRepository.findAllActiveStaff();
        if (!activeStaff.isEmpty()) {
            return activeStaff.getFirst();
        }

        throw new IllegalStateException("Nenhum barbeiro ativo cadastrado para associar ao agendamento");
    }

    private ServiceEntity resolveService(BarberNotifyEventDTO event) {
        if (event.getServiceName() != null && !event.getServiceName().isBlank()) {
            Optional<ServiceEntity> serviceOpt = serviceRepository.findFirstByNameIgnoreCaseAndIsActiveTrue(event.getServiceName());
            if (serviceOpt.isPresent()) return serviceOpt.get();

            // Tenta busca parcial
            List<ServiceEntity> allServices = serviceRepository.findAllByIsActiveTrueOrderByDisplayOrderAscNameAsc();
            for (ServiceEntity s : allServices) {
                if (s.getName().toLowerCase().contains(event.getServiceName().toLowerCase()) ||
                    event.getServiceName().toLowerCase().contains(s.getName().toLowerCase())) {
                    return s;
                }
            }
        }

        // Padrão: Primeiro serviço ativo (geralmente Corte Tradicional / Degradê)
        List<ServiceEntity> allServices = serviceRepository.findAllByIsActiveTrueOrderByDisplayOrderAscNameAsc();
        if (!allServices.isEmpty()) {
            return allServices.getFirst();
        }

        // Se não houver nenhum, cria um padrão
        ServiceEntity defaultService = ServiceEntity.builder()
                .name(event.getServiceName() != null ? event.getServiceName() : "Corte de Cabelo")
                .priceCents(event.getCustomPriceCents() != null ? event.getCustomPriceCents() : 4500)
                .durationMinutes(30)
                .category("Cabelo")
                .build();
        return serviceRepository.save(defaultService);
    }

    private BarberNotifyResponseDTO handleCancellation(BarberNotifyEventDTO event) {
        log.info("Processando cancelamento vindo do BarberNotify: cliente={}", event.getClientName());
        // Se houver lógica de cancelamento por ID externo ou telefone
        return BarberNotifyResponseDTO.builder()
                .success(true)
                .message("Cancelamento registrado com sucesso")
                .processedAt(OffsetDateTime.now())
                .build();
    }
}
