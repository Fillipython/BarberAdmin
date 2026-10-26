package br.com.barberadmin.service;

import br.com.barberadmin.domain.entity.User;
import br.com.barberadmin.domain.enums.UserRole;
import br.com.barberadmin.dto.staff.StaffDTO;
import br.com.barberadmin.exception.BusinessException;
import br.com.barberadmin.exception.ResourceNotFoundException;
import br.com.barberadmin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<StaffDTO> findAll() {
        return userRepository.findAllActiveStaff().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public StaffDTO create(StaffDTO dto) {
        String cleanPhone = dto.getPhone().replaceAll("[^0-9]", "");
        if (userRepository.findByPhoneAndDeletedAtIsNull(cleanPhone).isPresent()) {
            throw new BusinessException("Já existe um barbeiro cadastrado com este telefone.");
        }
        if (userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(dto.getEmail()).isPresent()) {
            throw new BusinessException("Já existe um barbeiro cadastrado com este email.");
        }

        User user = User.builder()
                .name(dto.getName())
                .nickname(dto.getNickname())
                .email(dto.getEmail().toLowerCase().trim())
                .phone(cleanPhone)
                .passwordHash("$2a$10$vDcq79ZJpD2bXyUu0tN7q.Zk9mQeH2eZ9t2eX9q.Zk9mQeH2eZ9t2") // Senha padrão inicial
                .role(dto.getRole() != null ? dto.getRole() : UserRole.BARBER)
                .commissionPercentage(dto.getCommissionPercentage())
                .avatarUrl(dto.getAvatarUrl())
                .bio(dto.getBio())
                .isActive(true)
                .build();

        return toDTO(userRepository.save(user));
    }

    @Transactional
    public StaffDTO update(UUID id, StaffDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Barbeiro não encontrado"));

        user.setName(dto.getName());
        user.setNickname(dto.getNickname());
        if (dto.getCommissionPercentage() != null) {
            user.setCommissionPercentage(dto.getCommissionPercentage());
        }
        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }
        if (dto.getIsActive() != null) {
            user.setIsActive(dto.getIsActive());
        }

        return toDTO(userRepository.save(user));
    }

    @Transactional
    public void delete(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Barbeiro não encontrado"));

        if (user.getRole() == UserRole.MASTER) {
            throw new BusinessException("O Barbeiro Master não pode ser removido do sistema.");
        }

        // Soft delete para manter integridade dos relatórios e recibos financeiros
        user.setDeletedAt(OffsetDateTime.now());
        user.setIsActive(false);
        userRepository.save(user);
    }

    private StaffDTO toDTO(User u) {
        return StaffDTO.builder()
                .id(u.getId())
                .name(u.getName())
                .nickname(u.getNickname())
                .email(u.getEmail())
                .phone(u.getPhone())
                .role(u.getRole())
                .commissionPercentage(u.getCommissionPercentage())
                .avatarUrl(u.getAvatarUrl())
                .bio(u.getBio())
                .isActive(u.getIsActive())
                .build();
    }
}
