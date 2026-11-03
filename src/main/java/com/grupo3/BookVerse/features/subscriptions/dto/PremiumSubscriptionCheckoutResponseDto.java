package com.grupo3.BookVerse.features.subscriptions.dto;

import com.grupo3.BookVerse.features.subscriptions.domain.UserSubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PremiumSubscriptionCheckoutResponseDto {

    private UUID userSubscriptionId;
    private UserSubscriptionStatus status;
    private BigDecimal amount;
    private String providerSubscriptionId;
    private String checkoutUrl;
}