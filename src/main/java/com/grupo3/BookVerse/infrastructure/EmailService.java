package com.grupo3.BookVerse.infrastructure;

public interface EmailService {

    void sendWelcomeEmail(String to, String username);

    void sendPasswordResetEmail(String to, String link, long expirationMinutes);

    void sendPasswordChangedEmail(String to);
}
