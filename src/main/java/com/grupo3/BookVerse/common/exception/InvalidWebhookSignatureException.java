package com.grupo3.BookVerse.common.exception;

public class InvalidWebhookSignatureException extends RuntimeException {

    public InvalidWebhookSignatureException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}