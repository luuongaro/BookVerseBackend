package com.grupo3.BookVerse.features.tips.dto;

import com.grupo3.BookVerse.features.payments.dto.MercadoPagoCheckoutResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TipCheckoutResponseDto {

    private TipResponseDto tip;
    private MercadoPagoCheckoutResponseDto checkout;
}