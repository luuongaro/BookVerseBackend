package com.grupo3.BookVerse.infrastructure.notifications;

import com.grupo3.BookVerse.infrastructure.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final MailDelivery delivery;

    @Value("${app.mail.from}")
    private String from;

    @Override
    @Async("emailTaskExecutor")
    public void sendWelcomeEmail(String to, String username) {
        sendMail(to, "Welcome to BookVerse fellow reader!",
                "Hi " + username + ",\n\n"
                + "Welcome to BookVerse! Your account has been created successfully.\n\n"
                + "Happy reading,\nThe BookVerse Team");
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendPasswordResetEmail(String to, String link, long expirationMinutes) {
        sendMail(to, "Restablecé tu contraseña de BookVerse",
                "Recibimos una solicitud para cambiar tu contraseña.\n\n"
                + "Abrí este enlace para elegir una nueva:\n" + link
                + "\n\nEl enlace vence en " + expirationMinutes + " minutos y se puede usar una sola vez."
                + "\n\nSi no lo pediste, podés ignorar este correo. Tu contraseña no cambió.");
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendPasswordChangedEmail(String to) {
        sendMail(to, "Tu contraseña de BookVerse cambió",
                "Se cambió la contraseña de tu cuenta de BookVerse.\n\n"
                + "Volvé a iniciar sesión con tu nueva contraseña.\n"
                + "Si no hiciste este cambio, solicitá recuperar tu contraseña desde BookVerse.");
    }

    private void sendMail(String to, String subject, String text) {
        try {
            var message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            delivery.send(message);
        } catch (MailDeliveryException exception) {
            log.error("Resend email delivery failed (HTTP status: {})", exception.getStatus());
        } catch (Exception exception) {
            // Never log a provider body, recipient, API key or recovery link.
            log.error("Email delivery failed ({})", exception.getClass().getSimpleName());
        }
    }
}
