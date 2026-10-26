package br.com.barberadmin.repository;

import br.com.barberadmin.domain.entity.FinancialRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, UUID> {

    @Query("SELECT f FROM FinancialRecord f WHERE f.paidAt >= :start AND f.paidAt < :end ORDER BY f.paidAt DESC")
    List<FinancialRecord> findAllInTimeRange(
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end
    );

    @Query("SELECT f FROM FinancialRecord f WHERE f.barber.id = :barberId AND f.paidAt >= :start AND f.paidAt < :end ORDER BY f.paidAt DESC")
    List<FinancialRecord> findByBarberInTimeRange(
            @Param("barberId") UUID barberId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end
    );

    // Agrupamento por método de pagamento (para o Donut Chart de Formas de Pagamento)
    @Query("SELECT f.paymentMethod, SUM(f.amountCents), COUNT(f.id) " +
           "FROM FinancialRecord f " +
           "WHERE f.paidAt >= :start AND f.paidAt < :end " +
           "GROUP BY f.paymentMethod")
    List<Object[]> sumByPaymentMethod(
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end
    );

    // Agrupamento por barbeiro (para o Donut Chart de Participação por Barbeiro)
    @Query("SELECT f.barber.id, f.barber.name, SUM(f.amountCents), SUM(f.barberCommissionCents), SUM(f.shopNetCents), COUNT(f.id) " +
           "FROM FinancialRecord f " +
           "WHERE f.paidAt >= :start AND f.paidAt < :end " +
           "GROUP BY f.barber.id, f.barber.name")
    List<Object[]> sumByBarber(
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end
    );
}
