package com.grupo3.BookVerse.infrastructure.notifications;

import com.grupo3.BookVerse.auth.events.PasswordChanged;
import com.grupo3.BookVerse.auth.events.PasswordResetRequested;
import com.grupo3.BookVerse.infrastructure.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordRecoveryMailListener {
    private final EmailService emails;

    @TransactionalEventListener
    public void resetRequested(PasswordResetRequested event) {
        try {
            emails.sendPasswordResetEmail(event.email(), event.link(), event.expirationMinutes());
        } catch (TaskRejectedException ignored) {
            log.error("Password recovery email queue is full");
        }
    }

    @TransactionalEventListener
    public void passwordChanged(PasswordChanged event) {
        try {
            emails.sendPasswordChangedEmail(event.email());
        } catch (TaskRejectedException ignored) {
            log.error("Password change email queue is full");
        }
    }
}
