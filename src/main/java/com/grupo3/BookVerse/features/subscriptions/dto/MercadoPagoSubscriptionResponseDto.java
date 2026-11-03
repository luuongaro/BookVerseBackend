package com.grupo3.BookVerse.features.subscriptions.dto;

public record MercadoPagoSubscriptionResponseDto(
        String providerSubscriptionId,
        String externalReference,
        String status,
        String checkoutUrl
) {
}