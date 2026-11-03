package com.grupo3.BookVerse.features.subscriptions.service;

import com.grupo3.BookVerse.features.subscriptions.dto.MercadoPagoSubscriptionResponseDto;

import java.math.BigDecimal;
import java.util.UUID;

public interface MercadoPagoSubscriptionService {

    MercadoPagoSubscriptionResponseDto createMonthlySubscription(
            String payerEmail,
            BigDecimal amount,
            UUID userSubscriptionId
    );

    MercadoPagoSubscriptionResponseDto getSubscription(
            String providerSubscriptionId
    );
}