package com.grupo3.BookVerse.features.stories.controller;

import com.grupo3.BookVerse.features.stories.dto.StoryDetailResponseDto;
import com.grupo3.BookVerse.features.stories.dto.StoryRequestDto;
import com.grupo3.BookVerse.features.stories.dto.StoryResponseDto;
import com.grupo3.BookVerse.features.stories.service.StoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
@Tag(
        name = "Stories",
        description = "Endpoints for managing user-created stories in BookVerse"
)
public class StoryController {

    private final StoryService storyService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get all stories",
            description = "Retrieves a paginated list of all active stories registered in the system.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stories retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    public ResponseEntity<Page<StoryResponseDto>> getAllStories(Pageable pageable) {
        return ResponseEntity.ok(storyService.getAllStories(pageable));
    }

    @GetMapping("/explore")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Explore stories by other authors",
            description = "Returns active, visible stories excluding the authenticated author's own stories before pagination.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Page<StoryResponseDto>> exploreStories(Pageable pageable) {
        return ResponseEntity.ok(storyService.getStoriesToExplore(pageable));
    }

    @GetMapping("/{idExternal}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get story by external id",
            description = "Retrieves an active story with its summarized chapters using its external UUID identifier.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Story retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Story not found", content = @Content)
    })
    public ResponseEntity<StoryDetailResponseDto> getStoryByIdExternal(
            @Parameter(
                    description = "External UUID of the story",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID idExternal
    ) {
        return ResponseEntity.ok(
                storyService.getStoryByIdExternal(idExternal)
        );
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Create a new story",
            description = "Creates a new story written by a user and returns the created resource.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Story created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Referenced story author not found", content = @Content)
    })
    public ResponseEntity<StoryResponseDto> createStory(
            @Valid @RequestBody StoryRequestDto storyRequestDto
    ) {
        return new ResponseEntity<>(
                storyService.createStory(storyRequestDto),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/{idExternal}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Update a story",
            description = "Updates an existing story identified by its external UUID.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Story updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Story not found", content = @Content)
    })
    public ResponseEntity<StoryResponseDto> updateStory(
            @Parameter(
                    description = "External UUID of the story to update",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID idExternal,
            @Valid @RequestBody StoryRequestDto storyRequestDto
    ) {
        return ResponseEntity.ok(
                storyService.updateStory(idExternal, storyRequestDto)
        );
    }

    @DeleteMapping("/{idExternal}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Delete a story",
            description = "Soft deletes an active story identified by its external UUID.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Story deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Story not found", content = @Content)
    })
    public ResponseEntity<Void> deleteStory(
            @Parameter(
                    description = "External UUID of the story to delete",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID idExternal
    ) {
        storyService.deleteStory(idExternal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/author/{authorId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get stories by author",
            description = "Retrieves a paginated list of all active stories created by the specified user acting as story author.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stories retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Story author not found", content = @Content)
    })
    public ResponseEntity<Page<StoryResponseDto>> getStoriesByAuthorId(
            @Parameter(
                    description = "External UUID of the user who authored the stories",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID authorId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                storyService.getStoriesByAuthorId(authorId, pageable)
        );
    }
}
