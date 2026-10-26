package br.com.barberadmin.repository;

import br.com.barberadmin.domain.entity.Appointment;
import br.com.barberadmin.domain.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    @Query("SELECT a FROM Appointment a WHERE a.scheduledAt >= :start AND a.scheduledAt < :end ORDER BY a.scheduledAt ASC")
    List<Appointment> findAllInTimeRange(
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end
    );

    @Query("SELECT a FROM Appointment a WHERE a.barber.id = :barberId AND a.scheduledAt >= :start AND a.scheduledAt < :end ORDER BY a.scheduledAt ASC")
    List<Appointment> findByBarberInTimeRange(
            @Param("barberId") UUID barberId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end
    );

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.barber.id = :barberId AND a.scheduledAt = :scheduledAt AND a.status NOT IN ('CANCELLED', 'NO_SHOW') AND (:excludeId IS NULL OR a.id != :excludeId)")
    boolean existsActiveSlotConflict(
            @Param("barberId") UUID barberId,
            @Param("scheduledAt") OffsetDateTime scheduledAt,
            @Param("excludeId") UUID excludeId
    );

    List<Appointment> findByClientIdOrderByScheduledAtDesc(UUID clientId);
}
