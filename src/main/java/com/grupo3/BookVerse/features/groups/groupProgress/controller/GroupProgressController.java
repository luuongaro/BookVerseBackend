package com.grupo3.BookVerse.features.groups.groupProgress.controller;


import com.grupo3.BookVerse.features.groups.groupProgress.dto.GroupProgressResponseDto;
import com.grupo3.BookVerse.features.groups.groupProgress.service.GroupProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/group-progress")
@Tag(
        name = "Group Progress",
        description = "Endpoints for calculating reading group progress against the active goal"
)
public class GroupProgressController {

    private final GroupProgressService groupProgressService;

    @GetMapping("/group/{groupId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Calculate group progress",
            description = """
                    Calculates the current progress of a reading group against its active goal.
                    The result is based on active group members and their reading statuses.
                    Only active group members, the group creator, an admin, or a moderator can access this endpoint.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Group progress calculated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid group state or incompatible progress normalization", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reading group or active goal not found", content = @Content)
    })
    public ResponseEntity<GroupProgressResponseDto> calculateProgress(
            @Parameter(
                    description = "External UUID of the reading group",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID groupId
    ) {
        return ResponseEntity.ok(
                groupProgressService.calculateProgress(groupId)
        );
    }


}
