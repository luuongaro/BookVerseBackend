package com.grupo3.BookVerse.infrastructure;

public interface EmailService {

    void sendWelcomeEmail(String to, String username);

    void sendPasswordResetEmail(String to, String link, long expirationMinutes);

    void sendPasswordChangedEmail(String to);

    void sendFriendRequestEmail(String to, String username, String sender);
    void sendGroupRequestEmail(String to, String username, String sender, String group);
    void sendPasswordResetEmail(String to, String username, String link, long expirationMinutes);
}
