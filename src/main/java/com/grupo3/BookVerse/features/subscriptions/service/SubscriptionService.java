package com.grupo3.BookVerse.features.subscriptions.service;

import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionType;
import com.grupo3.BookVerse.features.subscriptions.dto.PremiumSubscriptionCheckoutResponseDto;
import com.grupo3.BookVerse.features.subscriptions.dto.SubscriptionResponseDto;
import com.grupo3.BookVerse.features.subscriptions.dto.UserSubscriptionResponseDto;

import java.util.List;
import java.util.UUID;

public interface SubscriptionService {

    List<SubscriptionResponseDto> getAllSubscriptions();

    SubscriptionResponseDto getSubscriptionByIdExternal(
            UUID idExternal
    );

    SubscriptionResponseDto getAuthenticatedUserSubscription();

    SubscriptionResponseDto getUserSubscription(
            UUID userIdExternal
    );

    PremiumSubscriptionCheckoutResponseDto
    initiateAuthenticatedUserPremiumSubscription();

    SubscriptionResponseDto downgradeAuthenticatedUserToFree();

    SubscriptionResponseDto changeUserSubscription(
            UUID userIdExternal,
            SubscriptionType subscriptionType
    );

    UserSubscriptionResponseDto synchronizeAuthenticatedUserPremiumSubscription();
}