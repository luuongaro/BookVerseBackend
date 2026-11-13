package com.grupo3.BookVerse.features.groups.groupComment.controller;

import com.grupo3.BookVerse.features.groups.groupComment.dto.GroupCommentResponseDto;
import com.grupo3.BookVerse.features.groups.groupComment.dto.GroupCommentUpdateRequestDto;
import com.grupo3.BookVerse.features.groups.groupComment.dto.GroupCommentsVisibilityResponseDto;
import com.grupo3.BookVerse.features.groups.groupComment.service.GroupCommentService;
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
import com.grupo3.BookVerse.features.groups.groupComment.dto.GroupCommentRequestDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/group-comments")
@Tag(
        name = "Group Comments",
        description = "Endpoints for managing comments in reading groups"
)
public class GroupCommentController {

    private final GroupCommentService groupCommentService;

    @PostMapping("/group/{groupId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Create a new group comment",
            description = "Creates a comment within a reading group based on user's current progress.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Comment created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reading group or reading status not found", content = @Content)
    })
    public ResponseEntity<GroupCommentResponseDto> createComment(
            @Parameter(
                    description = "External UUID of the reading group",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID groupId,
            @Valid @RequestBody GroupCommentRequestDto requestDto
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(groupCommentService.createComment(groupId, requestDto));
    }

    @GetMapping("/group/{groupId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get visible comments by group",
            description = "Retrieves comments visible to the authenticated user based on their reading progress.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comments retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reading group not found", content = @Content)
    })
    public ResponseEntity<GroupCommentsVisibilityResponseDto> getVisibleCommentsByGroupId(
            @Parameter(
                    description = "External UUID of the reading group",
                    required = true
            )
            @PathVariable UUID groupId
    ) {
        return ResponseEntity.ok(
                groupCommentService.getVisibleCommentsByGroupId(groupId)
        );
    }

    @GetMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get comment by external id",
            description = "Retrieves a specific comment if the user has access to it.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comment retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Comment not found", content = @Content)
    })
    public ResponseEntity<GroupCommentResponseDto> getCommentByIdExternal(
            @PathVariable UUID commentId
    ) {
        return ResponseEntity.ok(
                groupCommentService.getCommentByIdExternal(commentId)
        );
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get comments by user",
            description = "Retrieves active comments created by a user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comments retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<List<GroupCommentResponseDto>> getActiveCommentsByUserId(
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(
                groupCommentService.getActiveCommentsByUserId(userId)
        );
    }

    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Delete a comment",
            description = "Deletes a comment logically (author or moderator/admin only).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Comment deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Comment not found", content = @Content)
    })
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID commentId
    ) {
        groupCommentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Update a group comment",
            description = """
                Updates an active group comment.
                Only the comment author can edit it.
                The updated spoiler progress cannot exceed
                the author's current reading progress.
                """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Comment updated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid comment state or progress",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Only the comment author can edit it",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Comment or reading status not found",
                    content = @Content
            )
    })
    public ResponseEntity<GroupCommentResponseDto> updateComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody GroupCommentUpdateRequestDto requestDto
    ) {
        return ResponseEntity.ok(
                groupCommentService.updateComment(
                        commentId,
                        requestDto
                )
        );
    }
}

