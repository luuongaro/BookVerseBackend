package com.grupo3.BookVerse.features.subscriptions.dto;

import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubscriptionChangeRequestDto {

    @NotNull
    private SubscriptionType type;
}
