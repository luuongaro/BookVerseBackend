package com.grupo3.BookVerse.infrastructure.notifications;

import org.springframework.mail.SimpleMailMessage;

public interface MailDelivery {
    void send(SimpleMailMessage message);
}
