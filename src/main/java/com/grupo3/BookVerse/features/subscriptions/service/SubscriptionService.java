package com.grupo3.BookVerse.features.subscriptions.service;

import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionType;
import com.grupo3.BookVerse.features.subscriptions.dto.SubscriptionResponseDto;

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

    SubscriptionResponseDto upgradeAuthenticatedUserToPremium();

    SubscriptionResponseDto downgradeAuthenticatedUserToFree();

    SubscriptionResponseDto changeUserSubscription(
            UUID userIdExternal,
            SubscriptionType subscriptionType
    );


}
