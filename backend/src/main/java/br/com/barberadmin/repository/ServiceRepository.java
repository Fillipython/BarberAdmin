package br.com.barberadmin.repository;

import br.com.barberadmin.domain.entity.ServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, UUID> {

    List<ServiceEntity> findAllByIsActiveTrueOrderByDisplayOrderAscNameAsc();

    Optional<ServiceEntity> findFirstByNameIgnoreCaseAndIsActiveTrue(String name);
    List<ServiceEntity> findByNameIgnoreCaseAndIsActiveTrue(String name);
}
