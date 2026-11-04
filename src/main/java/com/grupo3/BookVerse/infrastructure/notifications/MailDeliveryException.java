package com.grupo3.BookVerse.infrastructure.notifications;

public class MailDeliveryException extends RuntimeException {
    private final int status;

    public MailDeliveryException(int status) {
        super(status > 0 ? "Email provider rejected the request" : "Email provider connection failed");
        this.status = status;
    }

    public int getStatus() { return status; }
}
