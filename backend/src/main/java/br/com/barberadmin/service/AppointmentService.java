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
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
        if (callerRole == UserRole.BARBER && barberId != null) {
            list = appointmentRepository.findByBarberInTimeRange(barberId, startOfDay, endOfDay);
        } else if (barberId != null) {
            list = appointmentRepository.findByBarberInTimeRange(barberId, startOfDay, endOfDay);
        } else {
            list = appointmentRepository.findAllInTimeRange(startOfDay, endOfDay);
        }

        return list.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
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

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public AppointmentResponseDTO overridePrice(UUID appointmentId, PriceOverrideDTO dto) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessException("Não é possível alterar o valor de um agendamento já finalizado e pago.");
        }

        appointment.setFinalPriceCents(dto.getPriceCents());
        appointment.setTotalPriceCents(dto.getPriceCents());

        if (!appointment.getServices().isEmpty()) {
            appointment.getServices().getFirst().setPriceCentsAtBooking(dto.getPriceCents());
        }

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Valor do agendamento {} alterado para {} centavos", appointmentId, dto.getPriceCents());
        return toResponseDTO(saved);
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
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

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public AppointmentResponseDTO updateStatus(UUID appointmentId, AppointmentStatus status) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));

        appointment.setStatus(status);
        if (status == AppointmentStatus.COMPLETED) {
            appointment.setCompletedAt(OffsetDateTime.now());
        }
        Appointment saved = appointmentRepository.save(appointment);

        if (status == AppointmentStatus.CANCELLED && saved.getBarber() != null && saved.getScheduledAt() != null) {
            ZonedDateTime zdt = saved.getScheduledAt().atZoneSameInstant(ZONE_BR);
            String dateIso = zdt.toLocalDate().toString();
            String timeStr = zdt.format(TIME_FORMATTER);
            UUID bId = saved.getBarber().getId();
            syncCancelToBarberNotifyAsync(bId, dateIso, timeStr);
        }

        return toResponseDTO(saved);
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> closeDay(LocalDate date, UUID barberId, boolean closeShop, UUID callerId, UserRole callerRole) {
        if (callerRole != UserRole.MASTER) {
            if (closeShop) {
                throw new BusinessException("Apenas o Barbeiro Master pode fechar a loja inteira no dia.");
            }
            if (callerId != null && barberId != null && !callerId.equals(barberId)) {
                throw new BusinessException("Você só tem permissão para fechar o seu próprio horário.");
            }
            if (barberId == null) {
                barberId = callerId;
            }
        }

        List<User> targetBarbers;
        if (closeShop || barberId == null) {
            targetBarbers = userRepository.findAllActiveStaff();
        } else {
            User found = userRepository.findById(barberId)
                    .orElseThrow(() -> new ResourceNotFoundException("Barbeiro não encontrado"));
            targetBarbers = List.of(found);
        }

        Client blockClient = clientRepository.findFirstByPhone("11900000000")
                .orElseGet(() -> clientRepository.save(Client.builder()
                        .name("Agenda Fechada")
                        .phone("11900000000")
                        .email("agenda@barberman.com.br")
                        .notes("Registro interno de bloqueio de agenda")
                        .build()));

        OffsetDateTime blockStart = date.atTime(8, 0).atZone(ZONE_BR).toOffsetDateTime();
        OffsetDateTime blockEnd = date.atTime(20, 0).atZone(ZONE_BR).toOffsetDateTime();
        OffsetDateTime startOfDay = date.atStartOfDay(ZONE_BR).toOffsetDateTime();
        OffsetDateTime endOfDay = date.plusDays(1).atStartOfDay(ZONE_BR).toOffsetDateTime();

        String noteTag = closeShop ? "AGENDA_FECHADA_LOJA" : "AGENDA_FECHADA_BARBEIRO";

        for (User barber : targetBarbers) {
            List<Appointment> existingOnDay = appointmentRepository.findByBarberInTimeRange(
                    barber.getId(), startOfDay, endOfDay);

            Optional<Appointment> existingBlock = existingOnDay.stream()
                    .filter(a -> a.getNotes() != null && a.getNotes().contains("AGENDA_FECHADA"))
                    .findFirst();

            if (existingBlock.isPresent()) {
                Appointment blk = existingBlock.get();
                blk.setStatus(AppointmentStatus.CONFIRMED);
                blk.setScheduledAt(blockStart);
                blk.setEndAt(blockEnd);
                blk.setNotes(noteTag);
                appointmentRepository.save(blk);
            } else {
                Appointment blk = Appointment.builder()
                        .client(blockClient)
                        .barber(barber)
                        .scheduledAt(blockStart)
                        .endAt(blockEnd)
                        .status(AppointmentStatus.CONFIRMED)
                        .totalPriceCents(0)
                        .finalPriceCents(0)
                        .discountCents(0)
                        .notes(noteTag)
                        .build();
                appointmentRepository.save(blk);
            }
        }

        syncDayBlockToBarberNotifyAsync("POST", date.toString(), barberId, closeShop);
        return getDayBlockStatus(date);
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> reopenDay(LocalDate date, UUID barberId, boolean openShop, UUID callerId, UserRole callerRole) {
        if (callerRole != UserRole.MASTER) {
            if (openShop) {
                throw new BusinessException("Apenas o Barbeiro Master pode reabrir a loja inteira.");
            }
            if (callerId != null && barberId != null && !callerId.equals(barberId)) {
                throw new BusinessException("Você só tem permissão para reabrir o seu próprio horário.");
            }
            if (barberId == null) {
                barberId = callerId;
            }
        }

        OffsetDateTime startOfDay = date.atStartOfDay(ZONE_BR).toOffsetDateTime();
        OffsetDateTime endOfDay = date.plusDays(1).atStartOfDay(ZONE_BR).toOffsetDateTime();

        List<Appointment> activeOnDay = appointmentRepository.findAllInTimeRange(startOfDay, endOfDay);
        final UUID targetBarberId = barberId;

        for (Appointment apt : activeOnDay) {
            if (apt.getNotes() == null || !apt.getNotes().contains("AGENDA_FECHADA")) continue;
            if (callerRole != UserRole.MASTER && apt.getNotes().contains("AGENDA_FECHADA_LOJA")) {
                throw new BusinessException("A loja foi fechada pelo Barbeiro Master nesta data.");
            }
            if (openShop || targetBarberId == null || (apt.getBarber() != null && apt.getBarber().getId().equals(targetBarberId))) {
                apt.setStatus(AppointmentStatus.CANCELLED);
                appointmentRepository.save(apt);
            }
        }

        syncDayBlockToBarberNotifyAsync("DELETE", date.toString(), barberId, openShop);
        return getDayBlockStatus(date);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDayBlockStatus(LocalDate date) {
        OffsetDateTime startOfDay = date.atStartOfDay(ZONE_BR).toOffsetDateTime();
        OffsetDateTime endOfDay = date.plusDays(1).atStartOfDay(ZONE_BR).toOffsetDateTime();

        List<Appointment> activeOnDay = appointmentRepository.findAllInTimeRange(startOfDay, endOfDay);
        Set<String> closedBarberIds = new LinkedHashSet<>();
        boolean shopClosedByMaster = false;

        for (Appointment apt : activeOnDay) {
            if (apt.getStatus() == AppointmentStatus.CANCELLED || apt.getStatus() == AppointmentStatus.NO_SHOW) continue;
            if (apt.getNotes() != null && apt.getNotes().contains("AGENDA_FECHADA") && apt.getBarber() != null) {
                closedBarberIds.add(apt.getBarber().getId().toString());
                if (apt.getNotes().contains("AGENDA_FECHADA_LOJA")) {
                    shopClosedByMaster = true;
                }
            }
        }

        List<User> activeStaff = userRepository.findAllActiveStaff();
        boolean allClosed = !activeStaff.isEmpty() && activeStaff.stream()
                .allMatch(u -> closedBarberIds.contains(u.getId().toString()));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("date", date.toString());
        res.put("shopClosed", allClosed || shopClosedByMaster);
        res.put("shopClosedByMaster", shopClosedByMaster);
        res.put("closedBarberIds", new ArrayList<>(closedBarberIds));
        return res;
    }

    private void syncDayBlockToBarberNotifyAsync(String method, String dateIso, UUID barberId, boolean shopWide) {
        CompletableFuture.runAsync(() -> {
            String[] bases = {
                    "http://barber_man_backend:8081/api/appointments/day-block",
                    "http://host.docker.internal:8081/api/appointments/day-block",
                    "http://localhost:8081/api/appointments/day-block"
            };
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            for (String base : bases) {
                try {
                    HttpRequest req;
                    if ("POST".equalsIgnoreCase(method)) {
                        String payload = String.format(
                                "{\"date\":\"%s\",\"barberId\":%s,\"closeShop\":%b}",
                                dateIso,
                                barberId != null ? "\"" + barberId + "\"" : "null",
                                shopWide
                        );
                        req = HttpRequest.newBuilder()
                                .uri(URI.create(base))
                                .timeout(Duration.ofSeconds(3))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(payload))
                                .build();
                    } else {
                        String query = "?date=" + dateIso + "&openShop=" + shopWide + (barberId != null ? "&barberId=" + barberId : "");
                        req = HttpRequest.newBuilder()
                                .uri(URI.create(base + query))
                                .timeout(Duration.ofSeconds(3))
                                .DELETE()
                                .build();
                    }
                    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                        break;
                    }
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void syncCancelToBarberNotifyAsync(UUID barberId, String dateIso, String timeStr) {
        CompletableFuture.runAsync(() -> {
            String[] bases = {
                    "http://barber_man_backend:8081/api/appointments/by-slot",
                    "http://host.docker.internal:8081/api/appointments/by-slot",
                    "http://localhost:8081/api/appointments/by-slot"
            };
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            for (String base : bases) {
                try {
                    String query = String.format("?barberId=%s&date=%s&time=%s", barberId, dateIso, timeStr);
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(base + query))
                            .timeout(Duration.ofSeconds(3))
                            .DELETE()
                            .build();
                    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                        break;
                    }
                } catch (Exception ignored) {
                }
            }
        });
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
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

        User barber = appointment.getBarber();
        BigDecimal commissionRate = barber.getCommissionPercentage().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        int barberCommissionCents = new BigDecimal(finalPriceCents).multiply(commissionRate).setScale(0, RoundingMode.HALF_UP).intValue();
        int shopNetCents = finalPriceCents - barberCommissionCents;

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
        boolean isBlock = a.getNotes() != null && a.getNotes().contains("AGENDA_FECHADA");

        return AppointmentResponseDTO.builder()
                .id(a.getId())
                .clientId(a.getClient().getId())
                .clientName(isBlock ? "Agenda Fechada" : a.getClient().getName())
                .clientPhone(a.getClient().getPhone())
                .barberId(a.getBarber().getId())
                .barberName(a.getBarber().getName())
                .barberNickname(a.getBarber().getNickname())
                .serviceId(primaryService != null && primaryService.getService() != null ? primaryService.getService().getId() : null)
                .serviceName(isBlock
                        ? (a.getNotes().contains("LOJA") ? "Loja Fechada no Dia" : "Horário Fechado pelo Profissional")
                        : (primaryService != null ? primaryService.getServiceName() : "Serviço Geral"))
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
