package com.grupo3.BookVerse.features.status.controller;

import com.grupo3.BookVerse.features.status.dto.ReadingStatusRequestDto;
import com.grupo3.BookVerse.features.status.dto.ReadingStatusResponseDto;
import com.grupo3.BookVerse.features.status.service.ReadingStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reading-statuses")
@RequiredArgsConstructor
@Tag(
        name = "Reading Statuses",
        description = "Endpoints for managing user reading statuses for books and stories"
)
public class ReadingStatusController {

    private final ReadingStatusService readingStatusService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Create a new reading status",
            description = "Creates a new reading status for the authenticated user associated with either a book or a story.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reading status created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body, invalid progress data, missing book/story association, or subscription reading limit reached", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Book or story not found", content = @Content)
    })
    public ResponseEntity<ReadingStatusResponseDto> createReadingStatus(
            @Valid @RequestBody ReadingStatusRequestDto requestDto
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(readingStatusService.createReadingStatus(requestDto));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get all reading statuses",
            description = "Retrieves all reading statuses registered in the system. Accessible only to administrators and moderators.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reading statuses retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    public ResponseEntity<List<ReadingStatusResponseDto>> getAllReadingStatuses() {
        return ResponseEntity.ok(readingStatusService.getAllReadingStatuses());
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get my reading statuses",
            description = "Retrieves reading statuses for the authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Reading statuses retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            )
    })
    public ResponseEntity<List<ReadingStatusResponseDto>> getMyReadingStatuses() {
        return ResponseEntity.ok(readingStatusService.getMyReadingStatuses());
    }

    @GetMapping("/{idExternal}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get reading status by external id",
            description = "Retrieves a specific reading status by its external UUID. Accessible by the owner, an administrator, or a moderator.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reading status retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reading status not found", content = @Content)
    })
    public ResponseEntity<ReadingStatusResponseDto> getReadingStatusByIdExternal(
            @Parameter(
                    description = "External UUID of the reading status",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID idExternal
    ) {
        return ResponseEntity.ok(
                readingStatusService.getReadingStatusByIdExternal(idExternal)
        );
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get reading statuses by user",
            description = "Retrieves all reading statuses associated with a specific user using the user's external UUID. Accessible by the same user, an administrator, or a moderator.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reading statuses retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<List<ReadingStatusResponseDto>> getReadingStatusesByUser(
            @Parameter(
                    description = "External UUID of the user",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(
                readingStatusService.getReadingStatusesByUser(userId)
        );
    }

    @PutMapping("/{idExternal}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Update a reading status",
            description = "Updates an existing reading status by its external UUID. Accessible by the owner or an administrator.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reading status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body, invalid progress data, missing book/story association, or subscription reading limit reached", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reading status, book, or story not found", content = @Content)
    })
    public ResponseEntity<ReadingStatusResponseDto> updateReadingStatus(
            @Parameter(
                    description = "External UUID of the reading status to update",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID idExternal,
            @Valid @RequestBody ReadingStatusRequestDto requestDto
    ) {
        return ResponseEntity.ok(
                readingStatusService.updateReadingStatus(idExternal, requestDto)
        );
    }

    @DeleteMapping("/{idExternal}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Delete a reading status",
            description = "Deletes a reading status by its external UUID. Accessible by the owner or an administrator.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Reading status deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reading status not found", content = @Content)
    })
    public ResponseEntity<Void> deleteReadingStatus(
            @Parameter(
                    description = "External UUID of the reading status to delete",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID idExternal
    ) {
        readingStatusService.deleteReadingStatus(idExternal);
        return ResponseEntity.noContent().build();
    }
}