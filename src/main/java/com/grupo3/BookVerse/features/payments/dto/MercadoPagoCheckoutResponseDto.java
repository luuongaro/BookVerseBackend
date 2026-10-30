package com.grupo3.BookVerse.features.payments.dto;

public record MercadoPagoCheckoutResponseDto(
        String preferenceId,
        String checkoutUrl
) {
}