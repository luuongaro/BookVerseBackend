package com.grupo3.BookVerse.features.users.controller;

import com.grupo3.BookVerse.features.users.domain.ProfileBadgeType;
import com.grupo3.BookVerse.features.users.service.ProfileBadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/profiles/{id}/badge")
@RequiredArgsConstructor
public class ProfileBadgeController {
    private final ProfileBadgeService service;
    public record BadgeResponse(ProfileBadgeType badge) {}
    public record BadgeRequest(ProfileBadgeType badge) {}

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public BadgeResponse get(@PathVariable UUID id) { return new BadgeResponse(service.get(id)); }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public BadgeResponse update(@PathVariable UUID id, @RequestBody BadgeRequest request) {
        return new BadgeResponse(service.update(id, request.badge()));
    }
}
