package br.com.barberadmin.service;

import br.com.barberadmin.domain.entity.ServiceEntity;
import br.com.barberadmin.dto.service.ServiceDTO;
import br.com.barberadmin.exception.ResourceNotFoundException;
import br.com.barberadmin.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceCatalogService {

    private final ServiceRepository serviceRepository;

    @Transactional(readOnly = true)
    public List<ServiceDTO> findAll() {
        return serviceRepository.findAllByIsActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServiceDTO findById(UUID id) {
        ServiceEntity s = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado"));
        return toDTO(s);
    }

    @Transactional
    public ServiceDTO create(ServiceDTO dto) {
        ServiceEntity entity = ServiceEntity.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .category(dto.getCategory() != null ? dto.getCategory() : "Geral")
                .priceCents(dto.getPriceCents())
                .durationMinutes(dto.getDurationMinutes())
                .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
                .isActive(true)
                .build();

        return toDTO(serviceRepository.save(entity));
    }

    @Transactional
    public ServiceDTO update(UUID id, ServiceDTO dto) {
        ServiceEntity entity = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado"));

        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        if (dto.getCategory() != null) entity.setCategory(dto.getCategory());
        entity.setPriceCents(dto.getPriceCents());
        entity.setDurationMinutes(dto.getDurationMinutes());
        if (dto.getDisplayOrder() != null) entity.setDisplayOrder(dto.getDisplayOrder());
        if (dto.getIsActive() != null) entity.setIsActive(dto.getIsActive());

        return toDTO(serviceRepository.save(entity));
    }

    private ServiceDTO toDTO(ServiceEntity s) {
        return ServiceDTO.builder()
                .id(s.getId())
                .name(s.getName())
                .description(s.getDescription())
                .category(s.getCategory())
                .priceCents(s.getPriceCents())
                .priceFormatted(formatMoney(s.getPriceCents()))
                .durationMinutes(s.getDurationMinutes())
                .isActive(s.getIsActive())
                .displayOrder(s.getDisplayOrder())
                .build();
    }

    private String formatMoney(int cents) {
        double val = cents / 100.0;
        return String.format("R$ %.2f", val).replace(".", ",");
    }
}
