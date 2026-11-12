package com.grupo3.BookVerse.features.notifications.service.impl;

import com.grupo3.BookVerse.infrastructure.notifications.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.io.IOException;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ResendMailDeliveryTest {
    MockRestServiceServer server;
    ResendMailDelivery delivery;

    @BeforeEach void setup() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        delivery = new ResendMailDelivery(builder, "re_test_key");
    }
    @AfterEach void verifyRequests() { server.verify(); }

    SimpleMailMessage message() {
        var message = new SimpleMailMessage();
        message.setFrom("BookVerse <notificaciones@bookverse.com.ar>");
        message.setTo("reader@example.com");
        message.setSubject("Restablecé tu contraseña");
        message.setText("https://www.bookverse.com.ar/reset-password#token=test-token");
        return message;
    }

    @Test void postsTheExactMailOverHttpsWithServerAuthenticationAndAnIdempotencyKey() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer re_test_key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(request -> assertNotNull(UUID.fromString(request.getHeaders().getFirst("Idempotency-Key"))))
                .andExpect(content().json("""
                        {"from":"BookVerse <notificaciones@bookverse.com.ar>",
                         "to":["reader@example.com"],"subject":"Restablecé tu contraseña",
                         "text":"https://www.bookverse.com.ar/reset-password#token=test-token"}
                        """))
                .andRespond(withSuccess("{\"id\":\"sent-message\"}", MediaType.APPLICATION_JSON));
        assertDoesNotThrow(() -> delivery.send(message()));
    }

    @Test void sendsHtmlAlongsideThePlainText() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(content().json("""
                        {"text":"https://www.bookverse.com.ar/reset-password#token=test-token",
                         "html":"<p>Hola</p>"}
                        """))
                .andRespond(withSuccess("{\"id\":\"sent\"}", MediaType.APPLICATION_JSON));
        delivery.send(message(), "<p>Hola</p>");
    }

    @ParameterizedTest
    @ValueSource(ints = {307, 401, 403, 422, 429, 500, 503})
    void failedResponsesExposeOnlyTheStatusAndNeverTheProviderBody(int status) {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(withStatus(HttpStatusCode.valueOf(status))
                        .body("private-response-containing-a-token-and-an-api-key"));
        var failure = assertThrows(MailDeliveryException.class, () -> delivery.send(message()));
        assertEquals(status, failure.getStatus());
        assertFalse(failure.getMessage().contains("private-response"));
        assertNull(failure.getCause());
    }

    @Test void connectionFailuresDoNotExposeRequestData() {
        server.expect(requestTo("https://api.resend.com/emails"))
                .andRespond(request -> { throw new IOException("private-request-contents"); });
        var failure = assertThrows(MailDeliveryException.class, () -> delivery.send(message()));
        assertEquals(0, failure.getStatus());
        assertFalse(failure.getMessage().contains("private-request"));
        assertNull(failure.getCause());
    }

    @Test void missingOrMalformedKeysFailBeforeAnyNetworkRequest() {
        for (String key : new String[]{"", "  ", "re_private\nkey"}) {
            var failure = assertThrows(IllegalStateException.class,
                    () -> new ResendMailDelivery(RestClient.builder(), key));
            assertTrue(failure.getMessage().contains("RESEND_API_KEY"));
            assertFalse(failure.getMessage().contains("re_private"));
        }
    }
}
