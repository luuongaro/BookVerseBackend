package com.grupo3.BookVerse.features.subscriptions.repository;

import com.grupo3.BookVerse.features.subscriptions.domain.UserSubscriptionEntity;
import com.grupo3.BookVerse.features.subscriptions.domain.UserSubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSubscriptionRepository
        extends JpaRepository<UserSubscriptionEntity, Long> {

    Optional<UserSubscriptionEntity> findByIdExternal(UUID idExternal);

    Optional<UserSubscriptionEntity> findByProviderSubscriptionId(
            String providerSubscriptionId
    );

    Optional<UserSubscriptionEntity> findByUser_IdExternalAndStatus(
            UUID userIdExternal,
            UserSubscriptionStatus status
    );

    Optional<UserSubscriptionEntity>
    findFirstByUser_IdExternalAndStatusInOrderByCreatedAtDesc(
            UUID userIdExternal,
            Collection<UserSubscriptionStatus> statuses
    );
}