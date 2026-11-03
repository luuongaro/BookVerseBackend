package com.grupo3.BookVerse.config.mercadopago;

import com.grupo3.BookVerse.common.exception.InvalidWebhookSignatureException;
import com.grupo3.BookVerse.config.properties.MercadoPagoProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class MercadoPagoWebhookSignatureValidator {

    private static final String HMAC_SHA_256 = "HmacSHA256";

    private final MercadoPagoProperties properties;

    public void validate(
            String signatureHeader,
            String requestId,
            String dataId
    ) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new InvalidWebhookSignatureException(
                    "Mercado Pago webhook signature is missing"
            );
        }

        if (requestId == null || requestId.isBlank()) {
            throw new InvalidWebhookSignatureException(
                    "Mercado Pago request id is missing"
            );
        }

        if (dataId == null || dataId.isBlank()) {
            throw new InvalidWebhookSignatureException(
                    "Mercado Pago payment id is missing"
            );
        }

        String webhookSecret = properties.getWebhookSecret();

        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new InvalidWebhookSignatureException(
                    "Mercado Pago webhook secret is not configured"
            );
        }

        Map<String, String> signatureParts =
                parseSignatureHeader(signatureHeader);

        String timestamp = signatureParts.get("ts");
        String receivedSignature = signatureParts.get("v1");

        if (timestamp == null
                || timestamp.isBlank()
                || receivedSignature == null
                || receivedSignature.isBlank()) {
            throw new InvalidWebhookSignatureException(
                    "Invalid Mercado Pago webhook signature format"
            );
        }

        String manifest =
                "id:" + dataId.toLowerCase(Locale.ROOT)
                        + ";request-id:" + requestId
                        + ";ts:" + timestamp
                        + ";";

        String calculatedSignature =
                calculateSignature(manifest, webhookSecret);

        boolean valid = MessageDigest.isEqual(
                calculatedSignature.getBytes(StandardCharsets.UTF_8),
                receivedSignature.getBytes(StandardCharsets.UTF_8)
        );

        if (!valid) {
            throw new InvalidWebhookSignatureException(
                    "Invalid Mercado Pago webhook signature"
            );
        }
    }

    private Map<String, String> parseSignatureHeader(
            String signatureHeader
    ) {
        try {
            return Stream.of(signatureHeader.split(","))
                    .map(String::trim)
                    .map(part -> part.split("=", 2))
                    .filter(parts -> parts.length == 2)
                    .collect(Collectors.toMap(
                            parts -> parts[0],
                            parts -> parts[1],
                            (first, second) -> first
                    ));

        } catch (RuntimeException ex) {
            throw new InvalidWebhookSignatureException(
                    "Invalid Mercado Pago webhook signature format",
                    ex
            );
        }
    }

    private String calculateSignature(
            String manifest,
            String secret
    ) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);

            SecretKeySpec secretKey = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA_256
            );

            mac.init(secretKey);

            byte[] hash = mac.doFinal(
                    manifest.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (Exception ex) {
            throw new InvalidWebhookSignatureException(
                    "Could not validate Mercado Pago webhook signature",
                    ex
            );
        }
    }
}