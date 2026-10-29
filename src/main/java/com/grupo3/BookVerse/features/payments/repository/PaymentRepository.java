package com.grupo3.BookVerse.features.payments.repository;

import com.grupo3.BookVerse.features.payments.domain.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository
        extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findByIdExternal(UUID idExternal);

    Optional<PaymentEntity> findByProviderPaymentId(
            String providerPaymentId
    );

    Optional<PaymentEntity> findByProviderSubscriptionId(
            String providerSubscriptionId
    );
}