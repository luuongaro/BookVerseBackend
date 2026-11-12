package com.grupo3.BookVerse.features.friends;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/friendships") @PreAuthorize("isAuthenticated()")
public class FriendshipController {
    private final FriendshipService service;
    public FriendshipController(FriendshipService service) { this.service = service; }
    @GetMapping("/users/{userId}") public FriendshipDto relationship(@PathVariable UUID userId) { return service.relationship(userId); }
    @PostMapping("/users/{userId}") public ResponseEntity<FriendshipDto> request(@PathVariable UUID userId) { return ResponseEntity.status(201).body(service.request(userId)); }
    @GetMapping("/requests") public List<FriendshipDto> pending() { return service.pending(); }
    @GetMapping("/me") public List<FriendshipDto> friends() { return service.friends(); }
    @PostMapping("/{id}/accept") public ResponseEntity<Void> accept(@PathVariable UUID id) { service.respond(id, true); return ResponseEntity.noContent().build(); }
    @PostMapping("/{id}/reject") public ResponseEntity<Void> reject(@PathVariable UUID id) { service.respond(id, false); return ResponseEntity.noContent().build(); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> remove(@PathVariable UUID id) { service.remove(id); return ResponseEntity.noContent().build(); }
}
