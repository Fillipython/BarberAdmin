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
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Agendamentos", description = "Gestão da agenda, override de preços e checkout financeiro de cortes")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @Operation(summary = "Listar agendamentos por data", description = "Retorna os agendamentos do dia especificado. Permite filtrar por barbeiro e respeita a role (Master ou Barber)")
    @GetMapping
    public ResponseEntity<List<AppointmentResponseDTO>> listAppointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID barberId,
            @RequestParam(required = false, defaultValue = "MASTER") UserRole role) {

        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        List<AppointmentResponseDTO> list = appointmentService.findByDateAndBarber(targetDate, barberId, role);
        return ResponseEntity.ok(list);
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
