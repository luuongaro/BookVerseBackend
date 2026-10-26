package com.grupo3.BookVerse.features.subscriptions.service.impl;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.stories.domain.StoryAccessType;
import com.grupo3.BookVerse.features.stories.repository.StoryRepository;
import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionEntity;
import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionType;
import com.grupo3.BookVerse.features.subscriptions.dto.SubscriptionResponseDto;
import com.grupo3.BookVerse.features.subscriptions.mappers.SubscriptionMapper;
import com.grupo3.BookVerse.features.subscriptions.repository.SubscriptionRepository;
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
    public SubscriptionResponseDto upgradeAuthenticatedUserToPremium() {
        UserEntity authenticatedUser = getAuthenticatedUser();

        if (
                authenticatedUser.getSubscription() != null
                        && authenticatedUser.getSubscription().getType()
                        == SubscriptionType.PREMIUM
        ) {
            throw new BadRequestException(
                    "User already has a PREMIUM subscription"
            );
        }

        SubscriptionEntity premiumSubscription = subscriptionRepository.findByType(SubscriptionType.PREMIUM)
                .orElseThrow(() -> new ResourceNotFoundException("Premium subscription not found"));

        authenticatedUser.setSubscription(premiumSubscription);
        userRepository.save(authenticatedUser);

        return subscriptionMapper.toResponseDto(premiumSubscription);
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
}