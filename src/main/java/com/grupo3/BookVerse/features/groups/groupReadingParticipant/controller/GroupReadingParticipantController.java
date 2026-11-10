package com.grupo3.BookVerse.features.groups.groupReadingParticipant.controller;

import com.grupo3.BookVerse.features.groups.groupReadingParticipant.dto.GroupReadingParticipantResponseDto;
import com.grupo3.BookVerse.features.groups.groupReadingParticipant.service.GroupReadingParticipantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
        "/api/reading-groups/{groupId}/cycles/{cycleId}/participants"
)
@Tag(
        name = "Group Reading Participants",
        description = "Endpoints for participation in reading group cycles"
)
public class GroupReadingParticipantController {

    private final GroupReadingParticipantService participantService;

    @PostMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Join a reading cycle",
            description = """
                    Adds the authenticated group member as a participant
                    of an ACTIVE reading cycle.

                    The user must be an ACTIVE member of the group
                    and must have the cycle content in their library
                    with IN_PROGRESS, RE_READING, or FINISHED status.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Reading cycle participation created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid cycle state, missing library entry, or invalid reading status",
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
                    description = "Reading group, reading cycle, or membership not found",
                    content = @Content
            )
    })
    public ResponseEntity<GroupReadingParticipantResponseDto> joinCycle(
            @PathVariable UUID groupId,
            @PathVariable UUID cycleId
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        participantService.joinCycle(
                                groupId,
                                cycleId
                        )
                );
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get reading cycle participants",
            description = """
                    Retrieves the participants registered for a reading cycle.
                    Active group members, admins, and moderators can
                    view the participant list.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Reading cycle participants retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "User cannot view the participants of this group",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Reading group or reading cycle not found",
                    content = @Content
            )
    })
    public ResponseEntity<
            List<GroupReadingParticipantResponseDto>
            > getParticipants(
            @PathVariable UUID groupId,
            @PathVariable UUID cycleId
    ) {
        return ResponseEntity.ok(
                participantService.getParticipants(
                        groupId,
                        cycleId
                )
        );
    }

    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Withdraw from a reading cycle",
            description = """
                    Marks the authenticated user's ACTIVE participation
                    as WITHDRAWN without removing the user from the reading group.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Successfully withdrew from reading cycle"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Participation or reading cycle is not in a valid state",
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
                    description = "Reading group, reading cycle, membership, or participation not found",
                    content = @Content
            )
    })
    public ResponseEntity<Void> leaveCycle(
            @PathVariable UUID groupId,
            @PathVariable UUID cycleId
    ) {
        participantService.leaveCycle(
                groupId,
                cycleId
        );

        return ResponseEntity.noContent().build();
    }
}