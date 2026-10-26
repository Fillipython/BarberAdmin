package br.com.barberadmin.service;

import br.com.barberadmin.domain.entity.Client;
import br.com.barberadmin.dto.client.ClientDTO;
import br.com.barberadmin.exception.BusinessException;
import br.com.barberadmin.exception.ResourceNotFoundException;
import br.com.barberadmin.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public List<ClientDTO> findAll(String query) {
        List<Client> list = (query != null && !query.isBlank())
                ? clientRepository.searchClients(query.trim())
                : clientRepository.findAllByIsActiveTrueOrderByNameAsc();

        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClientDTO findById(UUID id) {
        Client c = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        return toDTO(c);
    }

    @Transactional
    public ClientDTO create(ClientDTO dto) {
        String cleanPhone = dto.getPhone().replaceAll("[^0-9]", "");
        if (clientRepository.findByPhone(cleanPhone).isPresent()) {
            throw new BusinessException("Já existe um cliente cadastrado com este telefone.");
        }

        Client client = Client.builder()
                .name(dto.getName())
                .phone(cleanPhone)
                .email(dto.getEmail())
                .notes(dto.getNotes())
                .build();

        return toDTO(clientRepository.save(client));
    }

    @Transactional
    public ClientDTO update(UUID id, ClientDTO dto) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));

        String cleanPhone = dto.getPhone().replaceAll("[^0-9]", "");
        clientRepository.findByPhone(cleanPhone).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BusinessException("Já existe outro cliente com este telefone.");
            }
        });

        client.setName(dto.getName());
        client.setPhone(cleanPhone);
        client.setEmail(dto.getEmail());
        client.setNotes(dto.getNotes());

        return toDTO(clientRepository.save(client));
    }

    private ClientDTO toDTO(Client c) {
        return ClientDTO.builder()
                .id(c.getId())
                .name(c.getName())
                .phone(c.getPhone())
                .email(c.getEmail())
                .notes(c.getNotes())
                .totalVisits(c.getTotalVisits())
                .totalSpentCents(c.getTotalSpentCents())
                .totalSpentFormatted(formatMoney(c.getTotalSpentCents()))
                .lastVisitAt(c.getLastVisitAt())
                .build();
    }

    private String formatMoney(long cents) {
        double val = cents / 100.0;
        return String.format("R$ %.2f", val).replace(".", ",");
    }
}
