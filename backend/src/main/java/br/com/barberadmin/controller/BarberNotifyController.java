package br.com.barberadmin.controller;

import br.com.barberadmin.dto.barbernotify.BarberNotifyEventDTO;
import br.com.barberadmin.dto.barbernotify.BarberNotifyResponseDTO;
import br.com.barberadmin.service.BarberNotifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/barbernotify")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "BarberNotify Integration", description = "Endpoints para ingestão de eventos, clientes e agendamentos vindos do BarberNotify")
public class BarberNotifyController {

    private final BarberNotifyService barberNotifyService;

    @Value("${barbernotify.webhook-secret:barbernotify_webhook_secret_2026}")
    private String configuredSecret;

    @Operation(summary = "Webhook receptor do BarberNotify", description = "Recebe eventos em tempo real do BarberNotify quando um cliente agenda, reagenda ou cancela um corte")
    @PostMapping("/webhook")
    public ResponseEntity<BarberNotifyResponseDTO> receiveWebhook(
            @RequestHeader(value = "X-BarberNotify-Secret", required = false) String secretHeader,
            @Valid @RequestBody BarberNotifyEventDTO event) {

        // Validação opcional de assinatura de segurança do Webhook
        if (secretHeader != null && !secretHeader.equals(configuredSecret)) {
            log.warn("Tentativa de webhook com secret inválido do BarberNotify!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        BarberNotifyResponseDTO response = barberNotifyService.processIncomingEvent(event);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Sincronização manual do BarberNotify", description = "Endpoint para teste ou ingestão direta de agendamento do BarberNotify")
    @PostMapping("/sync")
    public ResponseEntity<BarberNotifyResponseDTO> manualSync(
            @Valid @RequestBody BarberNotifyEventDTO event) {
        BarberNotifyResponseDTO response = barberNotifyService.processIncomingEvent(event);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Status da integração BarberNotify", description = "Verifica se o webhook receiver está ativo e operacional")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getIntegrationStatus() {
        return ResponseEntity.ok(Map.of(
                "status", "ONLINE",
                "service", "BarberNotify Integration Bridge",
                "timestamp", OffsetDateTime.now(),
                "endpoints", Map.of(
                        "webhook", "/api/v1/barbernotify/webhook",
                        "sync", "/api/v1/barbernotify/sync"
                )
        ));
    }
}
