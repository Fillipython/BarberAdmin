package br.com.barberadmin.controller;

import br.com.barberadmin.dto.client.ClientDTO;
import br.com.barberadmin.service.ClientService;
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
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Cadastro e histórico de clientes da barbearia")
public class ClientController {

    private final ClientService clientService;

    @Operation(summary = "Listar clientes", description = "Lista todos os clientes ou filtra por termo de busca (nome ou telefone)")
    @GetMapping
    public ResponseEntity<List<ClientDTO>> listClients(
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(clientService.findAll(search));
    }

    @Operation(summary = "Buscar cliente por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ClientDTO> getClient(@PathVariable UUID id) {
        return ResponseEntity.ok(clientService.findById(id));
    }

    @Operation(summary = "Cadastrar novo cliente")
    @PostMapping
    public ResponseEntity<ClientDTO> createClient(@Valid @RequestBody ClientDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(dto));
    }

    @Operation(summary = "Atualizar dados do cliente")
    @PutMapping("/{id}")
    public ResponseEntity<ClientDTO> updateClient(
            @PathVariable UUID id,
            @Valid @RequestBody ClientDTO dto) {
        return ResponseEntity.ok(clientService.update(id, dto));
    }
}
