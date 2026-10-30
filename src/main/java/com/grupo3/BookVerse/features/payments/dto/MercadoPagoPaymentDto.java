package com.grupo3.BookVerse.features.payments.dto;

public record MercadoPagoPaymentDto(
        String providerPaymentId,
        String externalReference,
        String status
) {
}