package br.com.barberadmin.controller;

import br.com.barberadmin.dto.service.ServiceDTO;
import br.com.barberadmin.service.ServiceCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
@Tag(name = "Catálogo de Serviços", description = "Serviços e preços oferecidos pela barbearia")
public class ServiceController {

    private final ServiceCatalogService serviceCatalogService;

    @Operation(summary = "Listar todos os serviços disponíveis")
    @GetMapping
    public ResponseEntity<List<ServiceDTO>> listServices() {
        return ResponseEntity.ok(serviceCatalogService.findAll());
    }

    @Operation(summary = "Cadastrar novo serviço")
    @PostMapping
    public ResponseEntity<ServiceDTO> createService(@Valid @RequestBody ServiceDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceCatalogService.create(dto));
    }

    @Operation(summary = "Atualizar serviço ou preço padrão")
    @PutMapping("/{id}")
    public ResponseEntity<ServiceDTO> updateService(
            @PathVariable UUID id,
            @Valid @RequestBody ServiceDTO dto) {
        return ResponseEntity.ok(serviceCatalogService.update(id, dto));
    }
}
