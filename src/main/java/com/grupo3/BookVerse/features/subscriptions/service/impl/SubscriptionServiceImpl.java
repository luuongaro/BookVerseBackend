package com.grupo3.BookVerse.features.subscriptions.service.impl;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.PaymentProviderException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.config.properties.PremiumSubscriptionProperties;
import com.grupo3.BookVerse.features.stories.domain.StoryAccessType;
import com.grupo3.BookVerse.features.stories.repository.StoryRepository;
import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionEntity;
import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionType;
import com.grupo3.BookVerse.features.subscriptions.domain.UserSubscriptionEntity;
import com.grupo3.BookVerse.features.subscriptions.domain.UserSubscriptionStatus;
import com.grupo3.BookVerse.features.subscriptions.dto.MercadoPagoSubscriptionResponseDto;
import com.grupo3.BookVerse.features.subscriptions.dto.PremiumSubscriptionCheckoutResponseDto;
import com.grupo3.BookVerse.features.subscriptions.dto.SubscriptionResponseDto;
import com.grupo3.BookVerse.features.subscriptions.dto.UserSubscriptionResponseDto;
import com.grupo3.BookVerse.features.subscriptions.mappers.SubscriptionMapper;
import com.grupo3.BookVerse.features.subscriptions.repository.SubscriptionRepository;
import com.grupo3.BookVerse.features.subscriptions.repository.UserSubscriptionRepository;
import com.grupo3.BookVerse.features.subscriptions.service.MercadoPagoSubscriptionService;
import com.grupo3.BookVerse.features.subscriptions.service.SubscriptionService;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionMapper subscriptionMapper;
    private final UserRepository userRepository;
    private final StoryRepository storyRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final MercadoPagoSubscriptionService mercadoPagoSubscriptionService;
    private final PremiumSubscriptionProperties premiumProperties;

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResponseDto> getAllSubscriptions() {
        return subscriptionMapper.toResponseDtoList(subscriptionRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponseDto getSubscriptionByIdExternal(UUID idExternal) {
        SubscriptionEntity entity = subscriptionRepository.findByIdExternal(idExternal)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        return subscriptionMapper.toResponseDto(entity);
    }

    @Override
    @Transactional
    public PremiumSubscriptionCheckoutResponseDto
    initiateAuthenticatedUserPremiumSubscription() {

        UserEntity authenticatedUser = getAuthenticatedUser();

        validateUserCanSubscribeToPremium(authenticatedUser);
        validatePremiumConfiguration();

        SubscriptionEntity premiumPlan =
                subscriptionRepository
                        .findByType(SubscriptionType.PREMIUM)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Premium subscription plan not found"
                                )
                        );

        UserSubscriptionEntity userSubscription =
                UserSubscriptionEntity.builder()
                        .user(authenticatedUser)
                        .subscription(premiumPlan)
                        .provider("MERCADO_PAGO")
                        .status(UserSubscriptionStatus.PENDING)
                        .build();

        UserSubscriptionEntity savedUserSubscription =
                userSubscriptionRepository.save(userSubscription);

        MercadoPagoSubscriptionResponseDto mercadoPagoSubscription =
                mercadoPagoSubscriptionService.createMonthlySubscription(
                        authenticatedUser.getEmail(),
                        premiumProperties.getPrice(),
                        savedUserSubscription.getIdExternal()
                );

        savedUserSubscription.setProviderSubscriptionId(
                mercadoPagoSubscription.providerSubscriptionId()
        );

        savedUserSubscription.setStatus(
                mapInitialSubscriptionStatus(
                        mercadoPagoSubscription.status()
                )
        );

        userSubscriptionRepository.save(savedUserSubscription);

        return PremiumSubscriptionCheckoutResponseDto.builder()
                .userSubscriptionId(
                        savedUserSubscription.getIdExternal()
                )
                .status(savedUserSubscription.getStatus())
                .amount(premiumProperties.getPrice())
                .providerSubscriptionId(
                        savedUserSubscription.getProviderSubscriptionId()
                )
                .checkoutUrl(
                        mercadoPagoSubscription.checkoutUrl()
                )
                .build();
    }

    private UserEntity getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserEntity user)) {
            throw new AccessDeniedException("Authenticated user not found");
        }

        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponseDto getAuthenticatedUserSubscription() {

        UserEntity authenticatedUser =
                getAuthenticatedUser();

        return subscriptionMapper.toResponseDto(
                authenticatedUser.getSubscription()
        );
    }

    @Override
    @Transactional
    public SubscriptionResponseDto downgradeAuthenticatedUserToFree() {

        UserEntity authenticatedUser = getAuthenticatedUser();

        if (
                authenticatedUser.getSubscription() != null
                        && authenticatedUser.getSubscription().getType()
                        == SubscriptionType.FREE
        ) {
            throw new BadRequestException(
                    "User already has a FREE subscription"
            );
        }

        SubscriptionEntity freeSubscription =
                subscriptionRepository
                        .findByType(SubscriptionType.FREE)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Free subscription not found"
                                )
                        );

        storyRepository.updateAccessTypeByAuthorId(
                authenticatedUser.getId(),
                StoryAccessType.PREMIUM,
                StoryAccessType.FREE
        );

        authenticatedUser.setSubscription(freeSubscription);

        userRepository.save(authenticatedUser);

        return subscriptionMapper.toResponseDto(
                freeSubscription
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponseDto getUserSubscription(
            UUID userIdExternal
    ) {
        UserEntity user =
                userRepository
                        .findByIdExternal(userIdExternal)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        return subscriptionMapper.toResponseDto(
                user.getSubscription()
        );
    }

    @Override
    @Transactional
    public SubscriptionResponseDto changeUserSubscription(
            UUID userIdExternal,
            SubscriptionType subscriptionType
    ) {
        UserEntity user = userRepository
                .findByIdExternal(userIdExternal)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "User not found"
                        )
                );

        if (
                user.getSubscription() != null
                        && user.getSubscription().getType()
                        == subscriptionType
        ) {
            throw new BadRequestException(
                    "User already has the selected subscription"
            );
        }

        SubscriptionEntity subscription =
                subscriptionRepository
                        .findByType(subscriptionType)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Subscription not found"
                                )
                        );

        if (subscriptionType == SubscriptionType.FREE) {
            storyRepository.updateAccessTypeByAuthorId(
                    user.getId(),
                    StoryAccessType.PREMIUM,
                    StoryAccessType.FREE
            );
        }

        user.setSubscription(subscription);

        userRepository.save(user);

        return subscriptionMapper.toResponseDto(
                subscription
        );
    }

    private void validateUserCanSubscribeToPremium(
            UserEntity user
    ) {
        if (user.getSubscription() != null
                && user.getSubscription().getType()
                == SubscriptionType.PREMIUM) {
            throw new BadRequestException(
                    "User already has a PREMIUM subscription"
            );
        }

        boolean hasPendingSubscription =
                userSubscriptionRepository
                        .findByUser_IdExternalAndStatus(
                                user.getIdExternal(),
                                UserSubscriptionStatus.PENDING
                        )
                        .isPresent();

        if (hasPendingSubscription) {
            throw new BadRequestException(
                    "User already has a pending PREMIUM subscription"
            );
        }

        boolean hasActiveSubscription =
                userSubscriptionRepository
                        .findByUser_IdExternalAndStatus(
                                user.getIdExternal(),
                                UserSubscriptionStatus.ACTIVE
                        )
                        .isPresent();

        if (hasActiveSubscription) {
            throw new BadRequestException(
                    "User already has an active PREMIUM subscription"
            );
        }
    }

    private void validatePremiumConfiguration() {
        if (premiumProperties.getPrice() == null
                || premiumProperties.getPrice().signum() <= 0) {
            throw new IllegalStateException(
                    "Premium subscription price is not configured correctly"
            );
        }

        if (premiumProperties.getBackUrl() == null
                || premiumProperties.getBackUrl().isBlank()) {
            throw new IllegalStateException(
                    "Premium subscription back URL is not configured"
            );
        }
    }

    private UserSubscriptionStatus mapInitialSubscriptionStatus(
            String providerStatus
    ) {
        if (providerStatus == null || providerStatus.isBlank()) {
            return UserSubscriptionStatus.PENDING;
        }

        return switch (providerStatus) {
            case "authorized" ->
                    UserSubscriptionStatus.ACTIVE;

            case "paused" ->
                    UserSubscriptionStatus.PAUSED;

            case "cancelled" ->
                    UserSubscriptionStatus.CANCELLED;

            case "pending" ->
                    UserSubscriptionStatus.PENDING;

            default ->
                    throw new BadRequestException(
                            "Unsupported Mercado Pago subscription status: "
                                    + providerStatus
                    );
        };
    }

    @Override
    @Transactional
    public UserSubscriptionResponseDto
    synchronizeAuthenticatedUserPremiumSubscription() {

        UserEntity authenticatedUser = getAuthenticatedUser();

        UserSubscriptionEntity userSubscription =
                userSubscriptionRepository
                        .findFirstByUser_IdExternalAndStatusInOrderByCreatedAtDesc(
                                authenticatedUser.getIdExternal(),
                                List.of(
                                        UserSubscriptionStatus.PENDING,
                                        UserSubscriptionStatus.ACTIVE,
                                        UserSubscriptionStatus.PAUSED
                                )
                        )
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "User premium subscription not found"
                                )
                        );

        if (userSubscription.getProviderSubscriptionId() == null
                || userSubscription.getProviderSubscriptionId().isBlank()) {
            throw new BadRequestException(
                    "User subscription does not have a Mercado Pago subscription id"
            );
        }

        MercadoPagoSubscriptionResponseDto providerSubscription =
                mercadoPagoSubscriptionService.getSubscription(
                        userSubscription.getProviderSubscriptionId()
                );

        validateExternalReference(
                userSubscription,
                providerSubscription.externalReference()
        );

        UserSubscriptionStatus newStatus =
                mapProviderSubscriptionStatus(
                        providerSubscription.status()
                );

        userSubscription.setStatus(newStatus);

        applySubscriptionPlan(
                authenticatedUser,
                userSubscription.getSubscription(),
                newStatus
        );

        userSubscriptionRepository.save(userSubscription);
        userRepository.save(authenticatedUser);

        return toUserSubscriptionResponse(userSubscription);
    }

    private void validateExternalReference(
            UserSubscriptionEntity userSubscription,
            String externalReference
    ) {
        if (externalReference == null
                || externalReference.isBlank()) {
            throw new PaymentProviderException(
                    "Mercado Pago subscription does not contain an external reference",
                    null
            );
        }

        if (!userSubscription.getIdExternal()
                .toString()
                .equals(externalReference)) {
            throw new PaymentProviderException(
                    "Mercado Pago subscription external reference does not match BookVerse subscription",
                    null
            );
        }
    }

    private UserSubscriptionStatus mapProviderSubscriptionStatus(
            String providerStatus
    ) {
        if (providerStatus == null || providerStatus.isBlank()) {
            throw new PaymentProviderException(
                    "Mercado Pago subscription does not contain a status",
                    null
            );
        }

        return switch (providerStatus) {
            case "authorized" ->
                    UserSubscriptionStatus.ACTIVE;

            case "pending" ->
                    UserSubscriptionStatus.PENDING;

            case "paused" ->
                    UserSubscriptionStatus.PAUSED;

            case "cancelled" ->
                    UserSubscriptionStatus.CANCELLED;

            default ->
                    throw new PaymentProviderException(
                            "Unsupported Mercado Pago subscription status: "
                                    + providerStatus,
                            null
                    );
        };
    }

    private void applySubscriptionPlan(
            UserEntity user,
            SubscriptionEntity premiumPlan,
            UserSubscriptionStatus status
    ) {
        if (status == UserSubscriptionStatus.ACTIVE) {
            user.setSubscription(premiumPlan);
            return;
        }

        if (status == UserSubscriptionStatus.PAUSED
                || status == UserSubscriptionStatus.CANCELLED) {

            SubscriptionEntity freePlan =
                    subscriptionRepository
                            .findByType(SubscriptionType.FREE)
                            .orElseThrow(
                                    () -> new ResourceNotFoundException(
                                            "Free subscription plan not found"
                                    )
                            );

            user.setSubscription(freePlan);

            storyRepository.updateAccessTypeByAuthorId(
                    user.getId(),
                    StoryAccessType.PREMIUM,
                    StoryAccessType.FREE
            );
        }
    }

    private UserSubscriptionResponseDto toUserSubscriptionResponse(
            UserSubscriptionEntity entity
    ) {
        return UserSubscriptionResponseDto.builder()
                .idExternal(entity.getIdExternal())
                .userId(entity.getUser().getIdExternal())
                .plan(entity.getSubscription().getType())
                .status(entity.getStatus())
                .provider(entity.getProvider())
                .providerSubscriptionId(
                        entity.getProviderSubscriptionId()
                )
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

}