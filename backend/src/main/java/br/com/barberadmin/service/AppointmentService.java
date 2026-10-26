package br.com.barberadmin.service;

import br.com.barberadmin.domain.entity.*;
import br.com.barberadmin.domain.enums.*;
import br.com.barberadmin.dto.appointment.*;
import br.com.barberadmin.exception.BusinessException;
import br.com.barberadmin.exception.ResourceNotFoundException;
import br.com.barberadmin.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final FinancialRecordRepository financialRecordRepository;

    private static final ZoneId ZONE_BR = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Transactional(readOnly = true)
    public List<AppointmentResponseDTO> findByDateAndBarber(LocalDate date, UUID barberId, UserRole callerRole) {
        OffsetDateTime startOfDay = date.atStartOfDay(ZONE_BR).toOffsetDateTime();
        OffsetDateTime endOfDay = date.plusDays(1).atStartOfDay(ZONE_BR).toOffsetDateTime();

        List<Appointment> list;
        // Se for BARBER comum, só pode ver a sua própria agenda
        if (callerRole == UserRole.BARBER && barberId != null) {
            list = appointmentRepository.findByBarberInTimeRange(barberId, startOfDay, endOfDay);
        } else if (barberId != null) {
            list = appointmentRepository.findByBarberInTimeRange(barberId, startOfDay, endOfDay);
        } else {
            list = appointmentRepository.findAllInTimeRange(startOfDay, endOfDay);
        }

        return list.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Transactional
    public AppointmentResponseDTO createAppointment(AppointmentRequestDTO dto) {
        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));

        User barber = userRepository.findById(dto.getBarberId())
                .orElseThrow(() -> new ResourceNotFoundException("Barbeiro não encontrado"));

        if (!barber.getIsActive() || barber.getDeletedAt() != null) {
            throw new BusinessException("O barbeiro selecionado está inativo.");
        }

        OffsetDateTime scheduledAt = dto.getScheduledAt();
        int duration = dto.getDurationMinutes() != null && dto.getDurationMinutes() > 0 ? dto.getDurationMinutes() : 30;
        OffsetDateTime endAt = scheduledAt.plusMinutes(duration);

        // Preço base ou customizado
        ServiceEntity primaryService = null;
        if (dto.getServiceIds() != null && !dto.getServiceIds().isEmpty()) {
            primaryService = serviceRepository.findById(dto.getServiceIds().getFirst())
                    .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado"));
        } else {
            primaryService = serviceRepository.findAllByIsActiveTrueOrderByDisplayOrderAscNameAsc().getFirst();
        }

        int finalPrice = dto.getCustomPriceCents() != null && dto.getCustomPriceCents() >= 0
                ? dto.getCustomPriceCents()
                : primaryService.getPriceCents();

        Appointment appointment = Appointment.builder()
                .client(client)
                .barber(barber)
                .scheduledAt(scheduledAt)
                .endAt(endAt)
                .status(AppointmentStatus.CONFIRMED)
                .totalPriceCents(finalPrice)
                .finalPriceCents(finalPrice)
                .discountCents(0)
                .notes(dto.getNotes())
                .build();

        AppointmentServiceItem item = AppointmentServiceItem.builder()
                .appointment(appointment)
                .service(primaryService)
                .serviceName(primaryService.getName())
                .priceCentsAtBooking(finalPrice)
                .durationMinutesAtBooking(primaryService.getDurationMinutes())
                .build();

        appointment.addService(item);
        Appointment saved = appointmentRepository.save(appointment);

        return toResponseDTO(saved);
    }

    /**
     * Permite ao Barbeiro Master ou ao barbeiro responsável alterar o valor de um corte diretamente
     */
    @Transactional
    public AppointmentResponseDTO overridePrice(UUID appointmentId, PriceOverrideDTO dto) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessException("Não é possível alterar o valor de um agendamento já finalizado e pago.");
        }

        appointment.setFinalPriceCents(dto.getPriceCents());
        appointment.setTotalPriceCents(dto.getPriceCents());

        // Atualiza também o snapshot do item de serviço
        if (!appointment.getServices().isEmpty()) {
            appointment.getServices().getFirst().setPriceCentsAtBooking(dto.getPriceCents());
        }

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Valor do agendamento {} alterado para {} centavos", appointmentId, dto.getPriceCents());
        return toResponseDTO(saved);
    }

    /**
     * Permite alterar o tipo de serviço associado ao agendamento
     */
    @Transactional
    public AppointmentResponseDTO changeService(UUID appointmentId, ServiceChangeDTO dto) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessException("Não é possível alterar o serviço de um agendamento já concluído.");
        }

        ServiceEntity newService = serviceRepository.findById(dto.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Novo serviço não encontrado"));

        int priceToApply = dto.getCustomPriceCents() != null && dto.getCustomPriceCents() >= 0
                ? dto.getCustomPriceCents()
                : newService.getPriceCents();

        appointment.getServices().clear();
        AppointmentServiceItem newItem = AppointmentServiceItem.builder()
                .appointment(appointment)
                .service(newService)
                .serviceName(newService.getName())
                .priceCentsAtBooking(priceToApply)
                .durationMinutesAtBooking(newService.getDurationMinutes())
                .build();
        appointment.addService(newItem);

        appointment.setTotalPriceCents(priceToApply);
        appointment.setFinalPriceCents(priceToApply);

        Appointment saved = appointmentRepository.save(appointment);
        return toResponseDTO(saved);
    }

    @Transactional
    public AppointmentResponseDTO updateStatus(UUID appointmentId, AppointmentStatus status) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        appointment.setStatus(status);
        if (status == AppointmentStatus.COMPLETED) {
            appointment.setCompletedAt(OffsetDateTime.now());
        }
        return toResponseDTO(appointmentRepository.save(appointment));
    }

    /**
     * Finaliza o atendimento, processa a comissão do barbeiro e registra no caixa da barbearia
     */
    @Transactional
    public AppointmentResponseDTO checkout(UUID appointmentId, CheckoutPaymentDTO dto) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessException("Este agendamento já foi finalizado anteriormente.");
        }

        int finalPriceCents = dto.getFinalAmountCents();
        appointment.setFinalPriceCents(finalPriceCents);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setCompletedAt(OffsetDateTime.now());
        if (dto.getNotes() != null && !dto.getNotes().isBlank()) {
            appointment.setNotes(appointment.getNotes() != null ? appointment.getNotes() + " | " + dto.getNotes() : dto.getNotes());
        }

        // Cálculo da comissão do barbeiro
        User barber = appointment.getBarber();
        BigDecimal commissionRate = barber.getCommissionPercentage().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        int barberCommissionCents = new BigDecimal(finalPriceCents).multiply(commissionRate).setScale(0, RoundingMode.HALF_UP).intValue();
        int shopNetCents = finalPriceCents - barberCommissionCents;

        // Registro Financeiro
        FinancialRecord record = FinancialRecord.builder()
                .appointment(appointment)
                .barber(barber)
                .client(appointment.getClient())
                .type(TransactionType.RECEITA_SERVICO)
                .amountCents(finalPriceCents)
                .barberCommissionCents(barberCommissionCents)
                .shopNetCents(shopNetCents)
                .paymentMethod(dto.getPaymentMethod())
                .paymentStatus(PaymentStatus.PAGO)
                .paidAt(OffsetDateTime.now())
                .description("Corte concluído - " + (appointment.getServices().isEmpty() ? "Serviço" : appointment.getServices().getFirst().getServiceName()))
                .build();

        financialRecordRepository.save(record);

        // Atualização de métricas do cliente
        Client client = appointment.getClient();
        client.setTotalVisits(client.getTotalVisits() + 1);
        client.setTotalSpentCents(client.getTotalSpentCents() + finalPriceCents);
        client.setLastVisitAt(OffsetDateTime.now());
        clientRepository.save(client);

        Appointment saved = appointmentRepository.save(appointment);
        return toResponseDTO(saved);
    }

    private AppointmentResponseDTO toResponseDTO(Appointment a) {
        AppointmentServiceItem primaryService = a.getServices().isEmpty() ? null : a.getServices().getFirst();
        String timeFormatted = a.getScheduledAt().atZoneSameInstant(ZONE_BR).format(TIME_FORMATTER);

        return AppointmentResponseDTO.builder()
                .id(a.getId())
                .clientId(a.getClient().getId())
                .clientName(a.getClient().getName())
                .clientPhone(a.getClient().getPhone())
                .barberId(a.getBarber().getId())
                .barberName(a.getBarber().getName())
                .barberNickname(a.getBarber().getNickname())
                .serviceId(primaryService != null && primaryService.getService() != null ? primaryService.getService().getId() : null)
                .serviceName(primaryService != null ? primaryService.getServiceName() : "Serviço Geral")
                .scheduledAt(a.getScheduledAt())
                .endAt(a.getEndAt())
                .timeFormatted(timeFormatted)
                .status(a.getStatus())
                .priceCents(a.getFinalPriceCents())
                .priceFormatted(formatMoney(a.getFinalPriceCents()))
                .notes(a.getNotes())
                .build();
    }

    private String formatMoney(Integer cents) {
        if (cents == null) return "R$ 0,00";
        double val = cents / 100.0;
        return String.format("R$ %.2f", val).replace(".", ",");
    }
}
