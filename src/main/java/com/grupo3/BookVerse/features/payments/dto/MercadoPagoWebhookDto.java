package com.grupo3.BookVerse.features.payments.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MercadoPagoWebhookDto {

    private String action;
    private String type;
    private WebhookData data;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class WebhookData {

        private String id;
    }
}