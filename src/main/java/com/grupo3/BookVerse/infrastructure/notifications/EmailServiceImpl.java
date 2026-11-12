package com.grupo3.BookVerse.infrastructure.notifications;

import com.grupo3.BookVerse.infrastructure.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final MailDelivery delivery;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.frontend-url:https://www.bookverse.com.ar}")
    private String frontendUrl;

    @Override
    @Async("emailTaskExecutor")
    public void sendWelcomeEmail(String to, String username) {
        String text = "Hola, " + username + ":\n\n¡Bienvenida a BookVerse! Tu cuenta fue creada correctamente.\n\n¡Feliz lectura!\nEl equipo de BookVerse";
        sendDesignedMail(to, "¡Bienvenida a BookVerse!", text, "Hola, " + escape(username) + ":", "¡Bienvenida a BookVerse! Tu cuenta fue creada correctamente.", null, null, "¡Feliz lectura!");
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendPasswordResetEmail(String to, String link, long expirationMinutes) {
        sendPasswordResetEmail(to, "", link, expirationMinutes);
    }

    @Override @Async("emailTaskExecutor")
    public void sendPasswordResetEmail(String to, String username, String link, long expirationMinutes) {
        String greeting = username == null || username.isBlank() ? "Hola:" : "Hola, " + username + ":";
        String safeGreeting = username == null || username.isBlank() ? "Hola:" : "Hola, " + escape(username) + ":";
        String body = "Recibimos una solicitud para cambiar la contraseña de tu cuenta de BookVerse.";
        String note = "El enlace vence en " + expirationMinutes + " minutos y se puede usar una sola vez. Si no hiciste esta solicitud, podés ignorar este mail. Tu contraseña seguirá siendo la misma.";
        sendDesignedMail(to, "Restablecé tu contraseña de BookVerse", greeting + "\n\n" + body + "\n\n" + link + "\n\n" + note + "\n\nEl equipo de BookVerse", safeGreeting, body, link, "Cambiar contraseña", note);
    }

    @Override @Async("emailTaskExecutor")
    public void sendFriendRequestEmail(String to, String username, String sender) {
        String body = sender + " quiere agregarte a sus amigos en BookVerse.";
        sendDesignedMail(to, "Tenés una solicitud de amistad", "Hola, " + username + ":\n\n" + body + "\n\nEl equipo de BookVerse", "Hola, " + escape(username) + ":", escape(sender) + " quiere agregarte a sus amigos en BookVerse.", null, null, null);
    }

    @Override @Async("emailTaskExecutor")
    public void sendGroupRequestEmail(String to, String username, String sender, String group) {
        String body = sender + " quiere unirse a tu grupo de lectura " + group + ".";
        sendDesignedMail(to, "Tenés una solicitud para tu grupo", "Hola, " + username + ":\n\n" + body + "\n\nEl equipo de BookVerse", "Hola, " + escape(username) + ":", escape(sender) + " quiere unirse a tu grupo de lectura <strong>" + escape(group) + "</strong>.", null, null, null);
    }

    private static String escape(String value) { return HtmlUtils.htmlEscape(value == null ? "" : value); }

    private void sendDesignedMail(String to, String subject, String text, String greeting, String body, String link, String label, String note) {
        String action = link == null ? "" : "<p><a href=\"" + escape(link) + "\" style=\"color:#665498\">" + escape(label) + "</a></p>";
        String extra = note == null ? "" : "<p>" + escape(note) + "</p>";
        String html = "<!doctype html><html lang=\"es\"><body style=\"margin:0;padding:24px;color:#302b43;font:16px/1.6 Georgia,serif\">"
                + "<p>" + greeting + "</p><p>" + body + "</p>" + action + extra
                + "<p>El equipo de BookVerse</p><img src=\"" + escape(frontendUrl) + "/assets/email/elephant.png\" width=\"120\" alt=\"Elefantito de BookVerse sobre un libro\" style=\"display:block;max-width:120px;height:auto\">"
                + "<p><img src=\"" + escape(frontendUrl) + "/assets/favicon.png\" width=\"22\" height=\"22\" alt=\"\" style=\"vertical-align:middle\"> <span style=\"color:#7465ad\">BookVerse</span></p></body></html>";
        // The welcome closing comes before the team signature.
        if ("¡Feliz lectura!".equals(note)) html = html.replace("<p>¡Feliz lectura!</p><p>El equipo", "<p>¡Feliz lectura!<br>El equipo");
        try {
            var message = new SimpleMailMessage(); message.setFrom(from); message.setTo(to); message.setSubject(subject); message.setText(text);
            delivery.send(message, html);
        } catch (MailDeliveryException exception) { log.error("Resend email delivery failed (HTTP status: {})", exception.getStatus()); }
        catch (Exception exception) { log.error("Email delivery failed ({})", exception.getClass().getSimpleName()); }
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
