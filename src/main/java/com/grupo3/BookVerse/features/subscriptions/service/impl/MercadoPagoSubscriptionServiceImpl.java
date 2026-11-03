package com.grupo3.BookVerse.features.subscriptions.service.impl;

import com.grupo3.BookVerse.common.exception.PaymentProviderException;
import com.grupo3.BookVerse.config.properties.PremiumSubscriptionProperties;
import com.grupo3.BookVerse.features.subscriptions.dto.MercadoPagoSubscriptionResponseDto;
import com.grupo3.BookVerse.features.subscriptions.service.MercadoPagoSubscriptionService;
import com.mercadopago.client.preapproval.PreApprovalAutoRecurringCreateRequest;
import com.mercadopago.client.preapproval.PreapprovalClient;
import com.mercadopago.client.preapproval.PreapprovalCreateRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preapproval.Preapproval;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MercadoPagoSubscriptionServiceImpl
        implements MercadoPagoSubscriptionService {

    private final PremiumSubscriptionProperties premiumProperties;

    @Override
    public MercadoPagoSubscriptionResponseDto createMonthlySubscription(
            String payerEmail,
            BigDecimal amount,
            UUID userSubscriptionId
    ) {
        try {
            PreApprovalAutoRecurringCreateRequest autoRecurring =
                    PreApprovalAutoRecurringCreateRequest.builder()
                            .frequency(1)
                            .frequencyType("months")
                            .transactionAmount(amount)
                            .currencyId("ARS")
                            .build();

            PreapprovalCreateRequest request =
                    PreapprovalCreateRequest.builder()
                            .payerEmail(payerEmail)
                            .reason("BookVerse Premium")
                            .externalReference(userSubscriptionId.toString())
                            .backUrl(premiumProperties.getBackUrl())
                            .status("pending")
                            .autoRecurring(autoRecurring)
                            .build();

            PreapprovalClient client = new PreapprovalClient();

            Preapproval subscription =
                    client.create(request);

            return new MercadoPagoSubscriptionResponseDto(
                    subscription.getId(),
                    subscription.getExternalReference(),
                    subscription.getStatus(),
                    subscription.getInitPoint()
            );

        } catch (MPApiException | MPException ex) {
            throw new PaymentProviderException(
                    "Error creating Mercado Pago premium subscription",
                    ex
            );
        }
    }

    @Override
    public MercadoPagoSubscriptionResponseDto getSubscription(
            String providerSubscriptionId
    ) {
        if (providerSubscriptionId == null
                || providerSubscriptionId.isBlank()) {
            throw new PaymentProviderException(
                    "Mercado Pago subscription id is required",
                    null
            );
        }

        try {
            PreapprovalClient client = new PreapprovalClient();

            Preapproval subscription =
                    client.get(providerSubscriptionId);

            return new MercadoPagoSubscriptionResponseDto(
                    subscription.getId(),
                    subscription.getExternalReference(),
                    subscription.getStatus(),
                    subscription.getInitPoint()
            );

        } catch (MPApiException | MPException ex) {
            throw new PaymentProviderException(
                    "Error retrieving Mercado Pago premium subscription",
                    ex
            );
        }
    }
}