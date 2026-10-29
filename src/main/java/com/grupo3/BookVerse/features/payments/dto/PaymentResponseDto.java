package com.grupo3.BookVerse.features.payments.dto;

import com.grupo3.BookVerse.features.payments.domain.PaymentStatus;
import com.grupo3.BookVerse.features.payments.domain.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponseDto(

        UUID paymentId,

        PaymentType type,

        PaymentStatus status,

        BigDecimal amount,

        String currency,

        String provider,

        LocalDateTime createdAt,

        LocalDateTime updatedAt,

        LocalDateTime approvedAt

) {
}