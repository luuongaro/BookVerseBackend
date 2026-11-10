package com.grupo3.BookVerse.features.groups.readingGroups.controller;

import com.grupo3.BookVerse.features.groups.readingGroups.dto.ReadingGroupDiscoveryResponseDto;
import com.grupo3.BookVerse.features.groups.readingGroups.dto.ReadingGroupRequestDto;
import com.grupo3.BookVerse.features.groups.readingGroups.dto.ReadingGroupResponseDto;
import com.grupo3.BookVerse.features.groups.readingGroups.dto.UpdateReadingGroupRequestDto;
import com.grupo3.BookVerse.features.groups.readingGroups.service.ReadingGroupService;
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
@RequestMapping("/api/reading-groups")
@RequiredArgsConstructor
@Tag(
        name = "Reading Groups",
        description = "Endpoints for managing reading groups"
)
public class ReadingGroupController {

    private final ReadingGroupService readingGroupService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Create a new reading group",
            description = "Creates a new reading group associated with exactly one book or one story. The creator is the authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reading group created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body, invalid content association, or duplicate active group name for the same content", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Book or story not found", content = @Content)
    })
    public ResponseEntity<ReadingGroupResponseDto> createGroup(
            @Valid @RequestBody ReadingGroupRequestDto dto
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(readingGroupService.createGroup(dto));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get all active reading groups",
            description = "Retrieves all active reading groups registered in the system.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reading groups retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    public ResponseEntity<List<ReadingGroupResponseDto>> getAllGroups() {
        return ResponseEntity.ok(readingGroupService.getAllGroups());
    }

    @GetMapping("/{idExternal}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get reading group by external id",
            description = "Retrieves an active reading group by its external UUID.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reading group retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reading group not found", content = @Content)
    })
    public ResponseEntity<ReadingGroupResponseDto> getGroupByIdExternal(
            @Parameter(
                    description = "External UUID of the reading group",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID idExternal
    ) {
        return ResponseEntity.ok(
                readingGroupService.getGroupByIdExternal(idExternal)
        );
    }

    @GetMapping("/book/{bookId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get reading groups by book",
            description = "Retrieves all active reading groups associated with a specific book using the book's external UUID.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reading groups retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Book not found", content = @Content)
    })
    public ResponseEntity<List<ReadingGroupResponseDto>> getGroupsByBook(
            @Parameter(
                    description = "External UUID of the book",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID bookId
    ) {
        return ResponseEntity.ok(
                readingGroupService.getGroupsByBookIdExternal(bookId)
        );
    }

    @GetMapping("/story/{storyId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get reading groups by story",
            description = "Retrieves all active reading groups associated with a specific story using the story's external UUID.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reading groups retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Story not found", content = @Content)
    })
    public ResponseEntity<List<ReadingGroupResponseDto>> getGroupsByStory(
            @Parameter(
                    description = "External UUID of the story",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID storyId
    ) {
        return ResponseEntity.ok(
                readingGroupService.getGroupsByStoryIdExternal(storyId)
        );
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get reading groups by creator",
            description = "Retrieves all active reading groups created by a specific user using the user's external UUID.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reading groups retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<List<ReadingGroupResponseDto>> getGroupsByUser(
            @Parameter(
                    description = "External UUID of the user",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(
                readingGroupService.getGroupsByUserIdExternal(userId)
        );
    }

    @PutMapping("/{idExternal}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Update a reading group",
            description = "Updates the name of an existing reading group by its external UUID. Only the creator, an admin, or a moderator can perform this action.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reading group updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body or duplicate active group name for the same content", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reading group not found", content = @Content)
    })
    public ResponseEntity<ReadingGroupResponseDto> updateGroup(
            @Parameter(
                    description = "External UUID of the reading group to update",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID idExternal,
            @Valid @RequestBody UpdateReadingGroupRequestDto dto
    ) {
        return ResponseEntity.ok(
                readingGroupService.updateGroup(idExternal, dto)
        );
    }

    @DeleteMapping("/{idExternal}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Deactivate a reading group",
            description = "Performs a logical deletion of a reading group by setting it as inactive. Only the creator, an admin, or a moderator can perform this action.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Reading group deactivated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reading group not found", content = @Content)
    })
    public ResponseEntity<Void> deleteGroup(
            @Parameter(
                    description = "External UUID of the reading group to deactivate",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID idExternal
    ) {
        readingGroupService.deleteGroup(idExternal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/discover")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Discover reading groups",
            description = "Retrieves active reading groups including their active reading cycle, active member count, and the authenticated user's membership state.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Reading groups retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            )
    })
    public ResponseEntity<List<ReadingGroupDiscoveryResponseDto>>
    discoverGroups() {

        return ResponseEntity.ok(
                readingGroupService.discoverGroups()
        );
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Search reading groups",
            description = "Searches active reading groups by name and includes the active reading cycle, active member count, and authenticated user's membership state.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Reading groups retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            )
    })
    public ResponseEntity<List<ReadingGroupDiscoveryResponseDto>> searchGroups(
            @RequestParam(required = false) String query
    ) {
        return ResponseEntity.ok(
                readingGroupService.searchGroups(query)
        );
    }
}