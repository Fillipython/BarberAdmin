package br.com.barberadmin.repository;

import br.com.barberadmin.domain.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {

    Optional<Client> findFirstByPhone(String phone);
    Optional<Client> findByPhone(String phone);

    Optional<Client> findByEmailIgnoreCase(String email);

    @Query("SELECT c FROM Client c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR c.phone LIKE CONCAT('%', :query, '%') ORDER BY c.name ASC")
    List<Client> searchClients(@Param("query") String query);

    List<Client> findAllByIsActiveTrueOrderByNameAsc();
}
