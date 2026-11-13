package com.grupo3.BookVerse.features.groups.groupComment.controller;

import com.grupo3.BookVerse.features.groups.groupComment.dto.GroupCommentsVisibilityResponseDto;
import com.grupo3.BookVerse.features.groups.groupComment.service.GroupCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(
        "/api/reading-groups/{groupId}/cycles/{cycleId}/comments"
)
@Tag(
        name = "Group Comments",
        description = "Endpoints for managing comments in reading group cycles"
)
public class GroupCycleCommentController {

    private final GroupCommentService groupCommentService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get visible comments by reading cycle",
            description = """
                    Retrieves active comments belonging to a specific reading cycle.
                    Active group members can view comments according to their
                    reading progress. Admins and moderators can view all active
                    comments for moderation.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Cycle comments retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "The reading cycle is not available for comments",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "User is not an active group member",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Reading group, cycle, or reading status not found",
                    content = @Content
            )
    })
    public ResponseEntity<GroupCommentsVisibilityResponseDto>
    getVisibleCommentsByCycleId(
            @PathVariable UUID groupId,
            @PathVariable UUID cycleId
    ) {
        return ResponseEntity.ok(
                groupCommentService.getVisibleCommentsByCycleId(
                        groupId,
                        cycleId
                )
        );
    }
}