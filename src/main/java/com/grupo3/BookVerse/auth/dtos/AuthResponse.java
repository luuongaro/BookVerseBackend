package com.grupo3.BookVerse.auth.dtos;

import java.util.UUID;

public record AuthResponse(
        String token,
        UUID userId
) {
}