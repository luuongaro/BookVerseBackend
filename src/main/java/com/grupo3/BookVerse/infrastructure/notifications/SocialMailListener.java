package com.grupo3.BookVerse.infrastructure.notifications;

import com.grupo3.BookVerse.infrastructure.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component @RequiredArgsConstructor @Slf4j
public class SocialMailListener {
    private final EmailService emails;
    @TransactionalEventListener
    public void onRequest(SocialMailRequested event) {
        try {
            if (event.group() == null) emails.sendFriendRequestEmail(event.email(), event.username(), event.sender());
            else emails.sendGroupRequestEmail(event.email(), event.username(), event.sender(), event.group());
        } catch (TaskRejectedException ignored) { log.error("Social email queue is full"); }
    }
}
