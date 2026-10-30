package com.grupo3.BookVerse.features.payments.service.impl;

import com.grupo3.BookVerse.common.exception.PaymentProviderException;
import com.grupo3.BookVerse.features.payments.dto.MercadoPagoCheckoutResponseDto;
import com.grupo3.BookVerse.features.payments.service.MercadoPagoService;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class MercadoPagoServiceImpl implements MercadoPagoService {

    @Override
    public MercadoPagoCheckoutResponseDto createCheckout(
            String title,
            BigDecimal amount,
            UUID paymentId
    ) {
        try {
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .title(title)
                    .quantity(1)
                    .currencyId("ARS")
                    .unitPrice(amount)
                    .build();

            PreferenceRequest request = PreferenceRequest.builder()
                    .items(List.of(item))
                    .externalReference(paymentId.toString())
                    .build();

            PreferenceClient client = new PreferenceClient();

            Preference preference = client.create(request);

            return new MercadoPagoCheckoutResponseDto(
                    preference.getId(),
                    preference.getInitPoint()
            );

        } catch (MPApiException | MPException ex) {
            throw new PaymentProviderException(
                    "Error creating Mercado Pago checkout",
                    ex
            );
        }
    }
}