package com.grupo3.BookVerse.features.tips.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TipRequestDto {

    @NotNull(message = "Story id is required")
    private UUID storyId;

    @NotNull(message = "Tip amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Tip amount must be greater than 0")
    private BigDecimal amount;

    @Size(max = 250, message = "Tip message must not exceed 250 characters")
    private String message;
}