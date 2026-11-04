package com.grupo3.BookVerse.auth.events;

public record PasswordResetRequested(String email, String link, long expirationMinutes) {}
