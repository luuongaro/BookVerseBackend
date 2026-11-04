package com.grupo3.BookVerse.infrastructure.notifications;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpMailDelivery implements MailDelivery {
    private final JavaMailSender sender;

    @Override
    public void send(SimpleMailMessage message) {
        sender.send(message);
    }
}
