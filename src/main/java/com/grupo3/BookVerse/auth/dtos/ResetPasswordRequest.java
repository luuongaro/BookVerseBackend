package com.grupo3.BookVerse.auth.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;
import java.nio.charset.StandardCharsets;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token,
        @NotBlank
        @Size(min = 8, max = 72)
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._-]).+$")
        String password
) {
    @AssertTrue(message = "Password exceeds the supported length")
    public boolean isPasswordLengthSupported() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
