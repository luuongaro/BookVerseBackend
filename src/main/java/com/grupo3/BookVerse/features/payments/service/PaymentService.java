package com.grupo3.BookVerse.features.payments.service;

import com.grupo3.BookVerse.features.payments.dto.PaymentResponseDto;

import java.util.UUID;

public interface PaymentService {

    PaymentResponseDto getPaymentByIdExternal(UUID idExternal);
}
