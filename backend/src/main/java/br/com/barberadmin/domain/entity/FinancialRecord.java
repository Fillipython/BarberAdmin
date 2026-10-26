package br.com.barberadmin.domain.entity;

import br.com.barberadmin.domain.enums.PaymentMethod;
import br.com.barberadmin.domain.enums.PaymentStatus;
import br.com.barberadmin.domain.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "financial_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "barber_id", nullable = false)
    private User barber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id")
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "transaction_type")
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private TransactionType type = TransactionType.RECEITA_SERVICO;

    @Column(name = "amount_cents", nullable = false)
    private Integer amountCents;

    @Column(name = "barber_commission_cents", nullable = false)
    @Builder.Default
    private Integer barberCommissionCents = 0;

    @Column(name = "shop_net_cents", nullable = false)
    @Builder.Default
    private Integer shopNetCents = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, columnDefinition = "payment_method")
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.PIX;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, columnDefinition = "payment_status")
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PAGO;

    @Column(name = "paid_at", nullable = false)
    @Builder.Default
    private OffsetDateTime paidAt = OffsetDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
