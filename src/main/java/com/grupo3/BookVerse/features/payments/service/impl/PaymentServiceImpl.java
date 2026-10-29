package com.grupo3.BookVerse.features.payments.service.impl;

import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.payments.domain.PaymentEntity;
import com.grupo3.BookVerse.features.payments.dto.PaymentResponseDto;
import com.grupo3.BookVerse.features.payments.mapper.PaymentMapper;
import com.grupo3.BookVerse.features.payments.repository.PaymentRepository;
import com.grupo3.BookVerse.features.payments.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentByIdExternal(UUID idExternal) {

        PaymentEntity payment = findPaymentByIdExternal(idExternal);

        return paymentMapper.toResponseDto(payment);
    }

    private PaymentEntity findPaymentByIdExternal(UUID idExternal) {

        return paymentRepository.findByIdExternal(idExternal)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment not found with idExternal: " + idExternal
                        )
                );
    }
}