package com.grupo3.BookVerse.features.tips.dto;

import com.grupo3.BookVerse.features.payments.domain.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TipResponseDto {

    private UUID idExternal;
    private UUID paymentId;
    private UUID storyId;
    private UUID senderUserId;
    private UUID receiverUserId;
    private PaymentStatus paymentStatus;
    private BigDecimal amount;
    private String message;
    private LocalDateTime createdAt;
}
