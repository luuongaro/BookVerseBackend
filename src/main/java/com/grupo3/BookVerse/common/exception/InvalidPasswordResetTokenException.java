package com.grupo3.BookVerse.common.exception;

public class InvalidPasswordResetTokenException extends RuntimeException {
    public InvalidPasswordResetTokenException() {
        super("El enlace venció o ya no es válido. Solicitá uno nuevo.");
    }
}
