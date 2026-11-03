package com.grupo3.BookVerse.features.subscriptions.dto;

import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionType;
import com.grupo3.BookVerse.features.subscriptions.domain.UserSubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubscriptionResponseDto {

    private UUID idExternal;
    private UUID userId;
    private SubscriptionType plan;
    private UserSubscriptionStatus status;
    private String provider;
    private String providerSubscriptionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}