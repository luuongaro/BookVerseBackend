package com.grupo3.BookVerse.features.payments.service.impl;

import com.grupo3.BookVerse.common.exception.PaymentProviderException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.payments.domain.PaymentEntity;
import com.grupo3.BookVerse.features.payments.domain.PaymentStatus;
import com.grupo3.BookVerse.features.payments.dto.MercadoPagoPaymentDto;
import com.grupo3.BookVerse.features.payments.dto.PaymentResponseDto;
import com.grupo3.BookVerse.features.payments.mapper.PaymentMapper;
import com.grupo3.BookVerse.features.payments.repository.PaymentRepository;
import com.grupo3.BookVerse.features.payments.service.MercadoPagoService;
import com.grupo3.BookVerse.features.payments.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final MercadoPagoService mercadoPagoService;

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentByIdExternal(UUID idExternal) {
        PaymentEntity payment = findPaymentByIdExternal(idExternal);

        return paymentMapper.toResponseDto(payment);
    }

    @Override
    @Transactional
    public void processMercadoPagoNotification(
            String providerPaymentId
    ) {
        MercadoPagoPaymentDto mercadoPagoPayment =
                mercadoPagoService.getPayment(providerPaymentId);

        UUID paymentIdExternal =
                parseExternalReference(
                        mercadoPagoPayment.externalReference()
                );

        PaymentEntity payment =
                findPaymentByIdExternal(paymentIdExternal);

        PaymentStatus newStatus =
                mapMercadoPagoStatus(
                        mercadoPagoPayment.status()
                );

        payment.setProviderPaymentId(
                mercadoPagoPayment.providerPaymentId()
        );

        payment.setStatus(newStatus);

        if (newStatus == PaymentStatus.APPROVED
                && payment.getApprovedAt() == null) {
            payment.setApprovedAt(LocalDateTime.now());
        }

        if (newStatus != PaymentStatus.APPROVED) {
            payment.setApprovedAt(null);
        }

        paymentRepository.save(payment);
    }

    private UUID parseExternalReference(
            String externalReference
    ) {
        if (externalReference == null
                || externalReference.isBlank()) {
            throw new PaymentProviderException(
                    "Mercado Pago payment does not contain an external reference",
                    null
            );
        }

        try {
            return UUID.fromString(externalReference);

        } catch (IllegalArgumentException ex) {
            throw new PaymentProviderException(
                    "Invalid Mercado Pago external reference",
                    ex
            );
        }
    }

    private PaymentStatus mapMercadoPagoStatus(
            String status
    ) {
        if (status == null || status.isBlank()) {
            throw new PaymentProviderException(
                    "Mercado Pago payment does not contain a status",
                    null
            );
        }

        return switch (status) {
            case "approved" ->
                    PaymentStatus.APPROVED;

            case "rejected" ->
                    PaymentStatus.REJECTED;

            case "cancelled" ->
                    PaymentStatus.CANCELLED;

            case "pending", "in_process", "authorized" ->
                    PaymentStatus.PENDING;

            default ->
                    throw new PaymentProviderException(
                            "Unsupported Mercado Pago payment status: "
                                    + status,
                            null
                    );
        };
    }

    private PaymentEntity findPaymentByIdExternal(
            UUID idExternal
    ) {
        return paymentRepository.findByIdExternal(idExternal)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment not found with idExternal: "
                                        + idExternal
                        )
                );
    }
}