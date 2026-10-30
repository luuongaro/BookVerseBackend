package com.grupo3.BookVerse.features.payments.controller;

import com.grupo3.BookVerse.features.payments.dto.MercadoPagoWebhookDto;
import com.grupo3.BookVerse.features.payments.service.MercadoPagoService;
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
        description = "Endpoints for managing BookVerse payments"
)
public class PaymentController {

    private final PaymentService paymentService;
    private final MercadoPagoService mercadoPagoService;

    @PostMapping("/mercado-pago/webhook")
    @Operation(
            summary = "Receive Mercado Pago payment notification",
            description = "Validates and processes Mercado Pago payment notifications."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Notification received successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid Mercado Pago webhook signature",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Error communicating with the payment provider",
                    content = @Content
            )
    })
    public ResponseEntity<Void> handleMercadoPagoWebhook(
            @RequestHeader("x-signature") String xSignature,
            @RequestHeader("x-request-id") String xRequestId,
            @RequestParam("data.id") String dataId,
            @RequestBody MercadoPagoWebhookDto webhook
    ) {

//        mercadoPagoService.validateWebhookSignature(
//                xSignature,
//                xRequestId,
//                dataId
//        );

//        if (webhook.getData() != null
//                && webhook.getData().getId() != null) {
//
//            paymentService.processMercadoPagoNotification(
//                    webhook.getData().getId()
//            );
//        }

        return ResponseEntity.ok().build();
    }
}