package com.grupo3.BookVerse.infrastructure.notifications;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ResendMailDelivery implements MailDelivery {
    private final RestClient client;

    public ResendMailDelivery(RestClient.Builder builder, String apiKey) {
        if (apiKey == null || apiKey.isBlank() || apiKey.contains("\n") || apiKey.contains("\r")) {
            throw new IllegalStateException("Configure RESEND_API_KEY when MAIL_PROVIDER=resend");
        }
        client = builder.baseUrl("https://api.resend.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.strip())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public void send(SimpleMailMessage message) {
        try {
            var response = client.post().uri("/emails")
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(Map.of("from", message.getFrom(), "to", List.of(message.getTo()),
                            "subject", message.getSubject(), "text", message.getText()))
                    .retrieve().toBodilessEntity();
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new MailDeliveryException(response.getStatusCode().value());
            }
        } catch (RestClientResponseException exception) {
            // Provider bodies and request headers may contain private mail content.
            throw new MailDeliveryException(exception.getStatusCode().value());
        } catch (RestClientException exception) {
            throw new MailDeliveryException(0);
        }
    }
}
