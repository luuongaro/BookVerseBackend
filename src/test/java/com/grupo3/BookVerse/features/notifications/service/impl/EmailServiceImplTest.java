package com.grupo3.BookVerse.features.notifications.service.impl;

import com.grupo3.BookVerse.infrastructure.notifications.EmailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import com.grupo3.BookVerse.infrastructure.notifications.MailDelivery;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private MailDelivery delivery;

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                emailService,
                "from",
                "bookverse@test.com"
        );
    }

    // Verify that the welcome email is sent correctly
    @Test
    void sendWelcomeEmail_shouldSendEmailSuccessfully() {

        String to = "user@test.com";
        String username = "Julieta";

        emailService.sendWelcomeEmail(to, username);

        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(delivery)
                .send(messageCaptor.capture());

        SimpleMailMessage sentMessage =
                messageCaptor.getValue();

        assertEquals(
                "bookverse@test.com",
                sentMessage.getFrom()
        );

        assertEquals(
                to,
                sentMessage.getTo()[0]
        );

        assertEquals(
                "Welcome to BookVerse fellow reader!",
                sentMessage.getSubject()
        );

        assertEquals(
                "Hi Julieta,\n\n" +
                        "Welcome to BookVerse! Your account has been created successfully.\n\n" +
                        "Happy reading,\n" +
                        "The BookVerse Team",
                sentMessage.getText()
        );
    }

    // Verify that no exception is thrown if email sending fails
    @Test
    void sendWelcomeEmail_shouldHandleException() {

        String to = "user@test.com";
        String username = "Julieta";

        doThrow(new RuntimeException("Mail error"))
                .when(delivery)
                .send(any(SimpleMailMessage.class));

        emailService.sendWelcomeEmail(to, username);

        verify(delivery)
                .send(any(SimpleMailMessage.class));
    }

    @Test
    void passwordRecoveryMailContainsTheExactLinkAndExpiration() {
        String link = "https://www.bookverse.com.ar/reset-password#token=" + "t".repeat(43);
        emailService.sendPasswordResetEmail("reader@example.com", link, 30);
        var captured = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(delivery).send(captured.capture());
        var mail = captured.getValue();
        assertEquals("reader@example.com", mail.getTo()[0]);
        assertEquals("bookverse@test.com", mail.getFrom());
        org.junit.jupiter.api.Assertions.assertTrue(mail.getText().contains(link));
        org.junit.jupiter.api.Assertions.assertTrue(mail.getText().contains("30 minutos"));
        org.junit.jupiter.api.Assertions.assertTrue(mail.getText().contains("una sola vez"));
    }

    @Test
    void passwordChangeNotificationDoesNotContainCredentialsOrARecoverySecret() {
        emailService.sendPasswordChangedEmail("reader@example.com");
        var captured = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(delivery).send(captured.capture());
        assertEquals("Tu contraseña de BookVerse cambió", captured.getValue().getSubject());
        org.junit.jupiter.api.Assertions.assertFalse(captured.getValue().getText().contains("token="));
    }
}
