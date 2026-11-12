package com.grupo3.BookVerse.infrastructure.notifications;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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

    @Override
    public void send(SimpleMailMessage message, String html) {
        try {
            var mime = sender.createMimeMessage();
            var helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(message.getFrom());
            helper.setTo(message.getTo());
            helper.setSubject(message.getSubject());
            helper.setText(message.getText(), html);
            sender.send(mime);
        } catch (jakarta.mail.MessagingException e) { throw new IllegalStateException("Could not create email", e); }
    }
}
