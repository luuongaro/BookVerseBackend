package com.grupo3.BookVerse.features.payments.controller;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.config.mercadopago.MercadoPagoWebhookSignatureValidator;
import com.grupo3.BookVerse.features.payments.dto.MercadoPagoWebhookDto;
import com.grupo3.BookVerse.features.payments.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(
        name = "Payments",
        description = "Endpoints for managing BookVerse payments and payment provider notifications"
)
public class PaymentController {

    private final PaymentService paymentService;
    private final MercadoPagoWebhookSignatureValidator signatureValidator;

    @PostMapping("/mercado-pago/webhook")
    @Operation(
            summary = "Receive Mercado Pago payment notification",
            description = "Validates a Mercado Pago webhook and updates the associated BookVerse payment using the provider payment id."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment notification processed successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid Mercado Pago notification",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid Mercado Pago webhook signature",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Associated BookVerse payment not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Error communicating with Mercado Pago",
                    content = @Content
            )
    })
    public ResponseEntity<Void> handleMercadoPagoWebhook(
            @RequestHeader("x-signature") String signature,
            @RequestHeader("x-request-id") String requestId,
            @RequestParam("data.id") String dataId,
            @RequestBody MercadoPagoWebhookDto webhook
    ) {
        validateNotification(webhook, dataId);

        signatureValidator.validate(
                signature,
                requestId,
                dataId
        );

        paymentService.processMercadoPagoNotification(dataId);

        return ResponseEntity.ok().build();
    }

    private void validateNotification(
            MercadoPagoWebhookDto webhook,
            String dataId
    ) {
        if (webhook.getData() == null
                || webhook.getData().getId() == null
                || webhook.getData().getId().isBlank()) {
            throw new BadRequestException(
                    "Mercado Pago notification does not contain a payment id"
            );
        }

        if (!dataId.equals(webhook.getData().getId())) {
            throw new BadRequestException(
                    "Mercado Pago payment id does not match notification body"
            );
        }
    }
}