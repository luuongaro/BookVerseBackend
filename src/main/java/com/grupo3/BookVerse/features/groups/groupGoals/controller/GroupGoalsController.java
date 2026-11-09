package com.grupo3.BookVerse.features.groups.groupGoals.controller;

import com.grupo3.BookVerse.features.groups.groupGoals.dto.GroupGoalStatusRequestDto;
import com.grupo3.BookVerse.features.groups.groupGoals.dto.GroupGoalsRequestDto;
import com.grupo3.BookVerse.features.groups.groupGoals.dto.GroupGoalsResponseDto;
import com.grupo3.BookVerse.features.groups.groupGoals.service.GroupGoalsService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequiredArgsConstructor
@RequestMapping(
        "/api/reading-groups/{groupId}/cycles/{cycleId}/goals"
)
@Tag(
        name = "Group Goals",
        description = "Endpoints for managing goals within reading group cycles"
)
public class GroupGoalsController {

    private final GroupGoalsService groupGoalsService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Create a reading cycle goal",
            description = """
                    Creates a new goal for an ACTIVE reading cycle.
                    A reading cycle can only have one ACTIVE goal at a time.
                    Percentage goals must exceed both the current group progress
                    and the highest completed percentage goal.
                    Only the group creator, an admin, or a moderator
                    can perform this action.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Reading cycle goal created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid goal, inactive cycle, invalid target, or active goal already exists",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Reading group or reading cycle not found",
                    content = @Content
            )
    })
    public ResponseEntity<GroupGoalsResponseDto> createGoal(
            @PathVariable UUID groupId,
            @PathVariable UUID cycleId,
            @Valid @RequestBody GroupGoalsRequestDto requestDto
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        groupGoalsService.createGoal(
                                groupId,
                                cycleId,
                                requestDto
                        )
                );
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get all reading cycle goals",
            description = """
                    Retrieves all goals associated with a reading cycle,
                    ordered by their last update.
                    Only active group members, the creator, admins,
                    or moderators can view them.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Reading cycle goals retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Reading group or reading cycle not found",
                    content = @Content
            )
    })
    public ResponseEntity<List<GroupGoalsResponseDto>> getGoalsByCycle(
            @PathVariable UUID groupId,
            @PathVariable UUID cycleId
    ) {
        return ResponseEntity.ok(
                groupGoalsService.getGoalsByCycleId(
                        groupId,
                        cycleId
                )
        );
    }

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get active reading cycle goal",
            description = """
                    Retrieves the current ACTIVE goal
                    associated with a reading cycle.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Active goal retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Reading group, reading cycle, or active goal not found",
                    content = @Content
            )
    })
    public ResponseEntity<GroupGoalsResponseDto> getActiveGoal(
            @PathVariable UUID groupId,
            @PathVariable UUID cycleId
    ) {
        return ResponseEntity.ok(
                groupGoalsService.getActiveGoalByCycleId(
                        groupId,
                        cycleId
                )
        );
    }

    @GetMapping("/{goalId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get reading cycle goal by external id",
            description = """
                    Retrieves a specific goal and validates that
                    the goal belongs to the reading cycle and group
                    identified in the URL.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Reading cycle goal retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Reading group, reading cycle, or goal not found",
                    content = @Content
            )
    })
    public ResponseEntity<GroupGoalsResponseDto> getGoalByIdExternal(
            @PathVariable UUID groupId,
            @PathVariable UUID cycleId,
            @PathVariable UUID goalId
    ) {
        return ResponseEntity.ok(
                groupGoalsService.getGoalByIdExternal(
                        groupId,
                        cycleId,
                        goalId
                )
        );
    }

    @PatchMapping("/{goalId}/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Change reading cycle goal status",
            description = """
                    Changes the status of an ACTIVE reading cycle goal.
                    Allowed transitions are ACTIVE to COMPLETED
                    and ACTIVE to CANCELLED.
                    Only the group creator, an admin, or a moderator
                    can perform this action.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Goal status updated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid goal status transition",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Reading group, reading cycle, or goal not found",
                    content = @Content
            )
    })
    public ResponseEntity<GroupGoalsResponseDto> changeStatus(
            @PathVariable UUID groupId,
            @PathVariable UUID cycleId,
            @PathVariable UUID goalId,
            @Valid @RequestBody GroupGoalStatusRequestDto requestDto
    ) {
        return ResponseEntity.ok(
                groupGoalsService.changeStatus(
                        groupId,
                        cycleId,
                        goalId,
                        requestDto
                )
        );
    }

    @DeleteMapping("/{goalId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Cancel an active reading cycle goal",
            description = """
                    Cancels an ACTIVE reading cycle goal.
                    The goal is not physically deleted, preserving
                    the reading cycle history.
                    Only the group creator, an admin, or a moderator
                    can perform this action.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Goal cancelled successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Only ACTIVE goals can be cancelled",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Reading group, reading cycle, or goal not found",
                    content = @Content
            )
    })
    public ResponseEntity<Void> cancelGoal(
            @PathVariable UUID groupId,
            @PathVariable UUID cycleId,
            @PathVariable UUID goalId
    ) {
        groupGoalsService.cancelGoal(
                groupId,
                cycleId,
                goalId
        );

        return ResponseEntity.noContent().build();
    }
}