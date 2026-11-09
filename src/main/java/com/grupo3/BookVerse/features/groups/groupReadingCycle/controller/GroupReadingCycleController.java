package com.grupo3.BookVerse.features.groups.groupReadingCycle.controller;

import com.grupo3.BookVerse.features.groups.groupReadingCycle.dto.GroupReadingCycleRequestDto;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.dto.GroupReadingCycleResponseDto;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.dto.UpdateGroupReadingCycleRequestDto;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.service.GroupReadingCycleService;
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
@RequestMapping("/api/reading-groups/{groupId}/cycles")
@RequiredArgsConstructor
@Tag(
        name = "Group Reading Cycles",
        description = "Endpoints for managing reading cycles within reading groups"
)
public class GroupReadingCycleController {

    private final GroupReadingCycleService groupReadingCycleService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Create a reading cycle",
            description = """
                    Creates a PLANNED reading cycle for a reading group.
                    The cycle must reference exactly one book or one story.
                    Book cycles require an edition reference and total chapters.
                    Only the group creator, an admin, or a moderator can perform this action.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reading cycle created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid reading cycle data", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Group, book, or story not found", content = @Content)
    })
    public ResponseEntity<GroupReadingCycleResponseDto> createCycle(
            @PathVariable UUID groupId,
            @Valid @RequestBody GroupReadingCycleRequestDto dto
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(groupReadingCycleService.createCycle(groupId, dto));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get reading cycles by group",
            description = "Retrieves the reading cycle history of a reading group.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<GroupReadingCycleResponseDto>> getCyclesByGroup(
            @PathVariable UUID groupId
    ) {
        return ResponseEntity.ok(
                groupReadingCycleService.getCyclesByGroupId(groupId)
        );
    }

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get active reading cycle",
            description = "Retrieves the current ACTIVE reading cycle of a group.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<GroupReadingCycleResponseDto> getActiveCycle(
            @PathVariable UUID groupId
    ) {
        return ResponseEntity.ok(
                groupReadingCycleService.getActiveCycleByGroupId(groupId)
        );
    }

    @GetMapping("/{cycleId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get reading cycle by external id",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<GroupReadingCycleResponseDto> getCycle(
            @PathVariable UUID groupId,
            @PathVariable UUID cycleId
    ) {
        return ResponseEntity.ok(
                groupReadingCycleService.getCycleByIdExternal(
                        groupId,
                        cycleId
                )
        );
    }

    @PutMapping("/{cycleId}/book-reference")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Update book edition and chapter reference",
            description = """
                    Updates the edition reference and total chapters of a book reading cycle.
                    Only the group creator, an admin, or a moderator can perform this action.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<GroupReadingCycleResponseDto> updateBookReference(
            @PathVariable UUID groupId,
            @PathVariable UUID cycleId,
            @Valid @RequestBody UpdateGroupReadingCycleRequestDto dto
    ) {
        return ResponseEntity.ok(
                groupReadingCycleService.updateBookReference(
                        groupId,
                        cycleId,
                        dto
                )
        );
    }

    @PatchMapping("/{cycleId}/start")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Start a reading cycle",
            description = """
                    Changes a reading cycle from PLANNED to ACTIVE.
                    A group can only have one ACTIVE reading cycle.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<GroupReadingCycleResponseDto> startCycle(
            @PathVariable UUID groupId,
            @PathVariable UUID cycleId
    ) {
        return ResponseEntity.ok(
                groupReadingCycleService.startCycle(
                        groupId,
                        cycleId
                )
        );
    }

    @PatchMapping("/{cycleId}/complete")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Complete a reading cycle",
            description = "Changes an ACTIVE reading cycle to COMPLETED without deactivating the reading group.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<GroupReadingCycleResponseDto> completeCycle(
            @PathVariable UUID groupId,
            @PathVariable UUID cycleId
    ) {
        return ResponseEntity.ok(
                groupReadingCycleService.completeCycle(
                        groupId,
                        cycleId
                )
        );
    }

    @PatchMapping("/{cycleId}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Cancel a reading cycle",
            description = "Changes a PLANNED or ACTIVE reading cycle to CANCELLED.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<GroupReadingCycleResponseDto> cancelCycle(
            @PathVariable UUID groupId,
            @PathVariable UUID cycleId
    ) {
        return ResponseEntity.ok(
                groupReadingCycleService.cancelCycle(
                        groupId,
                        cycleId
                )
        );
    }
}