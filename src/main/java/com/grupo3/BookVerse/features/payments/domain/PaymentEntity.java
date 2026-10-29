package com.grupo3.BookVerse.features.payments.domain;

import com.grupo3.BookVerse.features.users.domain.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "id_external",
            nullable = false,
            unique = true,
            updatable = false
    )
    private UUID idExternal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    private PaymentType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(
            name = "amount",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal amount;

    @Column(
            name = "currency",
            nullable = false,
            length = 3
    )
    private String currency;

    @Column(
            name = "provider",
            nullable = false,
            length = 50
    )
    private String provider;

    @Column(name = "provider_payment_id", unique = true)
    private String providerPaymentId;

    @Column(name = "provider_subscription_id", unique = true)
    private String providerSubscriptionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @PrePersist
    protected void onCreate() {
        if (idExternal == null) {
            idExternal = UUID.randomUUID();
        }

        if (status == null) {
            status = PaymentStatus.PENDING;
        }

        if (currency == null) {
            currency = "ARS";
        }

        if (provider == null) {
            provider = "MERCADO_PAGO";
        }

        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private void validateAmount() {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException(
                    "Payment amount must be greater than zero"
            );
        }
    }


}