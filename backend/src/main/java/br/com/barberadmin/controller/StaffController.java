package br.com.barberadmin.controller;

import br.com.barberadmin.dto.staff.StaffDTO;
import br.com.barberadmin.service.StaffService;
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
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
@Tag(name = "Equipe de Barbeiros", description = "Gestão de barbeiros e comissões")
public class StaffController {

    private final StaffService staffService;

    @Operation(summary = "Listar equipe de barbeiros ativos")
    @GetMapping
    public ResponseEntity<List<StaffDTO>> listStaff() {
        return ResponseEntity.ok(staffService.findAll());
    }

    @Operation(summary = "Adicionar novo barbeiro à equipe")
    @PostMapping
    public ResponseEntity<StaffDTO> createStaff(@Valid @RequestBody StaffDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(staffService.create(dto));
    }

    @Operation(summary = "Atualizar dados e percentual de comissão do barbeiro")
    @PutMapping("/{id}")
    public ResponseEntity<StaffDTO> updateStaff(
            @PathVariable UUID id,
            @Valid @RequestBody StaffDTO dto) {
        return ResponseEntity.ok(staffService.update(id, dto));
    }

    @Operation(summary = "Remover barbeiro (Soft Delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStaff(@PathVariable UUID id) {
        staffService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
