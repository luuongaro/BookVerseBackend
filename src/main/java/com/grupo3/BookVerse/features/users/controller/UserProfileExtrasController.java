package com.grupo3.BookVerse.features.users.controller;

import com.grupo3.BookVerse.features.books.dto.BookResponseDto;
import com.grupo3.BookVerse.features.reviews.dto.ReviewResponseDto;
import com.grupo3.BookVerse.features.users.dto.*;
import com.grupo3.BookVerse.features.users.service.UserProfileExtrasService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/profiles")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserProfileExtrasController {
    private final UserProfileExtrasService service;

    @GetMapping("/{id}/activity")
    public ProfileActivityDto activity(@PathVariable UUID id) { return service.activity(id); }

    @GetMapping("/{id}/appearance")
    public ProfileAppearanceDto appearance(@PathVariable UUID id) { return service.appearance(id); }

    @PutMapping("/me/appearance")
    public ProfileAppearanceDto appearance(@Valid @RequestBody ProfileAppearanceRequestDto request) {
        return service.updateAppearance(request);
    }

    @GetMapping("/{id}/favorites")
    public Page<UserProfileStoryDto> favorites(@PathVariable UUID id,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "12") int size) {
        return service.favorites(id, PageRequest.of(Math.max(0, page), Math.max(1, Math.min(24, size))));
    }

    @GetMapping("/{id}/favorite-books")
    public Page<BookResponseDto> favoriteBooks(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return service.favoriteBooks(
                id,
                PageRequest.of(
                        Math.max(0, page),
                        Math.max(1, Math.min(24, size))
                )
        );
    }

    @GetMapping("/{id}/reviews")
    public List<ReviewResponseDto> reviews(@PathVariable UUID id) {
        return service.reviews(id);
    }

    @GetMapping("/me/favorites/{storyId}")
    public Map<String, Boolean> isFavorite(@PathVariable UUID storyId) {
        return Map.of("favorite", service.isFavorite(storyId));
    }

    @PutMapping("/me/favorites/{storyId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addFavorite(@PathVariable UUID storyId) { service.addFavorite(storyId); }

    @GetMapping("/me/favorite-books/{bookId}")
    public Map<String, Boolean> isFavoriteBook(@PathVariable UUID bookId) {
        return Map.of(
                "favorite",
                service.isFavoriteBook(bookId)
        );
    }

    @PutMapping("/me/favorite-books/{bookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addFavoriteBook(@PathVariable UUID bookId) {
        service.addFavoriteBook(bookId);
    }

    @DeleteMapping("/me/favorites/{storyId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFavorite(@PathVariable UUID storyId) { service.removeFavorite(storyId); }

    @DeleteMapping("/me/favorite-books/{bookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFavoriteBook(@PathVariable UUID bookId) {
        service.removeFavoriteBook(bookId);
    }
}
