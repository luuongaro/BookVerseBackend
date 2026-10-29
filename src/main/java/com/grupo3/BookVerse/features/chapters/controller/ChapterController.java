package com.grupo3.BookVerse.features.chapters.controller;

import com.grupo3.BookVerse.features.chapters.dto.ChapterCreateRequestDto;
import com.grupo3.BookVerse.features.chapters.dto.ChapterResponseDto;
import com.grupo3.BookVerse.features.chapters.dto.ChapterUpdateRequestDto;
import com.grupo3.BookVerse.features.chapters.service.ChapterService;
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
@RequestMapping("/api/chapters")
@RequiredArgsConstructor
@Tag(
        name = "Chapters",
        description = "Endpoints for managing story chapters in BookVerse"
)
public class ChapterController {

    private final ChapterService chapterService;

    @PostMapping
    @Operation(
            summary = "Create a new chapter",
            description = "Creates a new chapter for an existing story. Chapter number is assigned automatically per story.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Chapter created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Story not found", content = @Content)
    })
    public ResponseEntity<ChapterResponseDto> createChapter(
            @Valid @RequestBody ChapterCreateRequestDto chapterCreateRequestDto
    ) {
        return new ResponseEntity<>(
                chapterService.createChapter(chapterCreateRequestDto),
                HttpStatus.CREATED
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    @Operation(
            summary = "Get all chapters",
            description = "Retrieves a paginated list of all active chapters in the system. Available only to administrators and moderators.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chapters retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied. Administrator or moderator role required", content = @Content
    )
    })
    public ResponseEntity<Page<ChapterResponseDto>> getAllChapters(Pageable pageable) {
        return ResponseEntity.ok(chapterService.getAllChapters(pageable));
    }

    @GetMapping("/{idExternal}")
    @Operation(
            summary = "Get chapter by external id",
            description = "Retrieves an active chapter by its external UUID identifier.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chapter retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied due to story subscription restrictions or unpublished chapter", content = @Content),
            @ApiResponse(responseCode = "404", description = "Chapter not found", content = @Content)
    })

    public ResponseEntity<ChapterResponseDto> getChapterByIdExternal(
            @Parameter(
                    description = "External UUID of the chapter",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID idExternal
    ) {
        return ResponseEntity.ok(
                chapterService.getChapterByIdExternal(idExternal)
        );
    }

    @GetMapping("/story/{storyId}")
    @Operation(
            summary = "Get chapters by story",
            description = "Retrieves a paginated list of all active chapters for a specific active story, ordered by chapter number ascending.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chapters retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Premium subscription required to access the chapters of this story", content = @Content),
            @ApiResponse(responseCode = "404", description = "Story not found", content = @Content)
    })
    public ResponseEntity<Page<ChapterResponseDto>> getChaptersByStoryId(
            @Parameter(
                    description = "External UUID of the story",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID storyId,
            @Parameter(description = "Return published chapters only, including for the author and moderators")
            @RequestParam(defaultValue = "false") boolean publishedOnly,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                chapterService.getChaptersByStoryId(storyId, pageable, publishedOnly)
        );
    }

    @PutMapping("/{idExternal}")
    @Operation(
            summary = "Update a chapter",
            description = "Updates an existing chapter. Story association and chapter number cannot be changed.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chapter updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Chapter not found", content = @Content)
    })
    public ResponseEntity<ChapterResponseDto> updateChapter(
            @Parameter(
                    description = "External UUID of the chapter to update",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID idExternal,
            @Valid @RequestBody ChapterUpdateRequestDto chapterUpdateRequestDto
    ) {
        return ResponseEntity.ok(
                chapterService.updateChapter(idExternal, chapterUpdateRequestDto)
        );
    }

    @DeleteMapping("/{idExternal}")
    @Operation(
            summary = "Delete a chapter",
            description = "Soft deletes an active chapter identified by its external UUID.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Chapter deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Chapter not found", content = @Content)
    })
    public ResponseEntity<Void> deleteChapter(
            @Parameter(
                    description = "External UUID of the chapter to delete",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID idExternal
    ) {
        chapterService.deleteChapter(idExternal);
        return ResponseEntity.noContent().build();
    }
}
