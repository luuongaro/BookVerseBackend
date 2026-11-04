package com.grupo3.BookVerse.auth.controller;

import com.grupo3.BookVerse.auth.dtos.ForgotPasswordRequest;
import com.grupo3.BookVerse.auth.dtos.ResetPasswordRequest;
import com.grupo3.BookVerse.auth.service.PasswordRecoveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordRecoveryController {
    private final PasswordRecoveryService recovery;

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            recovery.requestReset(request.email());
        } catch (TaskRejectedException ignored) {
            // Keep the same response under load; never reveal whether an account exists.
        }
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        recovery.resetPassword(request.token(), request.password());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
