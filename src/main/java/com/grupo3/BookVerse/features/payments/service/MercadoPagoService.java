package com.grupo3.BookVerse.features.payments.service;

import com.grupo3.BookVerse.features.payments.dto.MercadoPagoCheckoutResponseDto;

import java.math.BigDecimal;
import java.util.UUID;

public interface MercadoPagoService {

    MercadoPagoCheckoutResponseDto createCheckout(
            String title,
            BigDecimal amount,
            UUID paymentId
    );
}