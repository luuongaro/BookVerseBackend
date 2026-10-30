package com.grupo3.BookVerse.features.users.controller;

import com.grupo3.BookVerse.features.users.dto.UserProfileResponseDto;
import com.grupo3.BookVerse.features.users.dto.UserProfileStoryDto;
import com.grupo3.BookVerse.features.users.service.UserProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/profiles")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "User profiles", description = "Community profiles visible to authenticated users")
public class UserProfileController {
    private final UserProfileService service;

    @GetMapping
    public Page<UserProfileResponseDto> search(@RequestParam(defaultValue = "") String username,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "12") int size) {
        return service.search(username, pagination(page, size));
    }

    @GetMapping("/{id}")
    public UserProfileResponseDto getProfile(@PathVariable UUID id) {
        return service.getProfile(id);
    }

    @GetMapping("/{id}/stories")
    public Page<UserProfileStoryDto> getStories(@PathVariable UUID id,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "12") int size) {
        return service.getStories(id, pagination(page, size));
    }

    private PageRequest pagination(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(24, size)));
    }
}
