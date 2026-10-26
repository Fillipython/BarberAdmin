package br.com.barberadmin.repository;

import br.com.barberadmin.domain.entity.User;
import br.com.barberadmin.domain.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    Optional<User> findByPhoneAndDeletedAtIsNull(String phone);

    List<User> findAllByDeletedAtIsNullOrderByRoleAscNameAsc();

    List<User> findByRoleAndDeletedAtIsNullAndIsActiveTrue(UserRole role);

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND u.isActive = true ORDER BY u.role ASC, u.name ASC")
    List<User> findAllActiveStaff();
}
