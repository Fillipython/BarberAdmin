package br.com.barberadmin.controller;

import br.com.barberadmin.domain.enums.AppointmentStatus;
import br.com.barberadmin.domain.enums.UserRole;
import br.com.barberadmin.dto.appointment.*;
import br.com.barberadmin.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Agendamentos", description = "Gestão da agenda, override de preços, fechamento de horários e checkout financeiro de cortes")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private static final ZoneId ZONE_BR = ZoneId.of("America/Sao_Paulo");

    @Operation(summary = "Listar agendamentos por data", description = "Retorna os agendamentos do dia especificado. Permite filtrar por barbeiro e respeita a role (Master ou Barber)")
    @GetMapping
    public ResponseEntity<List<AppointmentResponseDTO>> listAppointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID barberId,
            @RequestParam(required = false, defaultValue = "MASTER") UserRole role) {

        LocalDate targetDate = (date != null) ? date : LocalDate.now(ZONE_BR);
        List<AppointmentResponseDTO> list = appointmentService.findByDateAndBarber(targetDate, barberId, role);
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Consultar status de fechamento da loja ou agendas no dia")
    @GetMapping("/day-block")
    public ResponseEntity<Map<String, Object>> getDayBlockStatus(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now(ZONE_BR);
        return ResponseEntity.ok(appointmentService.getDayBlockStatus(targetDate));
    }

    @Operation(summary = "Fechar agenda do dia (Loja inteira pelo Master ou agenda individual)")
    @PostMapping("/day-block")
    public ResponseEntity<Map<String, Object>> closeDay(
            @RequestBody Map<String, Object> body) {
        String dateStr = String.valueOf(body.getOrDefault("date", LocalDate.now(ZONE_BR).toString()));
        LocalDate targetDate = LocalDate.parse(dateStr);
        String barberIdStr = body.get("barberId") != null ? String.valueOf(body.get("barberId")) : null;
        String callerIdStr = body.get("callerId") != null ? String.valueOf(body.get("callerId")) : null;
        String roleStr = String.valueOf(body.getOrDefault("callerRole", "BARBER"));
        boolean closeShop = Boolean.parseBoolean(String.valueOf(body.getOrDefault("closeShop", "false")));

        UUID barberId = (barberIdStr != null && !barberIdStr.isBlank() && !"null".equalsIgnoreCase(barberIdStr))
                ? UUID.fromString(barberIdStr)
                : null;
        UUID callerId = (callerIdStr != null && !callerIdStr.isBlank() && !"null".equalsIgnoreCase(callerIdStr))
                ? UUID.fromString(callerIdStr)
                : barberId;
        UserRole callerRole = "MASTER".equalsIgnoreCase(roleStr) ? UserRole.MASTER : UserRole.BARBER;

        return ResponseEntity.ok(appointmentService.closeDay(targetDate, barberId, closeShop, callerId, callerRole));
    }

    @Operation(summary = "Reabrir agenda do dia (Loja inteira pelo Master ou agenda individual)")
    @DeleteMapping("/day-block")
    public ResponseEntity<Map<String, Object>> reopenDay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID barberId,
            @RequestParam(defaultValue = "false") boolean openShop,
            @RequestParam(required = false) UUID callerId,
            @RequestParam(defaultValue = "BARBER") UserRole callerRole) {
        return ResponseEntity.ok(appointmentService.reopenDay(date, barberId, openShop, callerId, callerRole));
    }

    @Operation(summary = "Criar novo agendamento manual")
    @PostMapping
    public ResponseEntity<AppointmentResponseDTO> createAppointment(
            @Valid @RequestBody AppointmentRequestDTO dto) {
        AppointmentResponseDTO created = appointmentService.createAppointment(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Alterar o valor do agendamento (Override de Preço)", description = "Permite ao Master ou Barbeiro ajustar o preço cobrado diretamente no agendamento")
    @PatchMapping("/{id}/price")
    public ResponseEntity<AppointmentResponseDTO> overridePrice(
            @PathVariable UUID id,
            @Valid @RequestBody PriceOverrideDTO dto) {
        AppointmentResponseDTO updated = appointmentService.overridePrice(id, dto);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Alterar o serviço do agendamento", description = "Permite trocar o tipo de serviço (ex: Corte para Barba/Combo)")
    @PatchMapping("/{id}/service")
    public ResponseEntity<AppointmentResponseDTO> changeService(
            @PathVariable UUID id,
            @Valid @RequestBody ServiceChangeDTO dto) {
        AppointmentResponseDTO updated = appointmentService.changeService(id, dto);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Atualizar status do agendamento (CONFIRMED, CANCELLED, NO_SHOW)")
    @PatchMapping("/{id}/status")
    public ResponseEntity<AppointmentResponseDTO> updateStatus(
            @PathVariable UUID id,
            @RequestParam AppointmentStatus status) {
        AppointmentResponseDTO updated = appointmentService.updateStatus(id, status);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Finalizar atendimento (Checkout)", description = "Marca o corte como concluído, registra no financeiro e calcula a comissão do barbeiro")
    @PostMapping("/{id}/checkout")
    public ResponseEntity<AppointmentResponseDTO> checkout(
            @PathVariable UUID id,
            @Valid @RequestBody CheckoutPaymentDTO dto) {
        AppointmentResponseDTO completed = appointmentService.checkout(id, dto);
        return ResponseEntity.ok(completed);
    }
}
