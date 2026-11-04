package com.grupo3.BookVerse.features.notifications.service.impl;

import com.grupo3.BookVerse.infrastructure.notifications.SmtpMailDelivery;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmtpMailDeliveryTest {
    @Test void localSmtpDelegatesToTheExistingMailSender() {
        var sender = mock(JavaMailSender.class);
        var message = new SimpleMailMessage();
        new SmtpMailDelivery(sender).send(message);
        verify(sender).send(message);
    }

    @Test void smtpFailuresReachTheServiceForSafeLogging() {
        var sender = mock(JavaMailSender.class);
        doThrow(new IllegalStateException("failure")).when(sender).send(any(SimpleMailMessage.class));
        assertThrows(IllegalStateException.class,
                () -> new SmtpMailDelivery(sender).send(new SimpleMailMessage()));
    }
}
