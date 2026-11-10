package com.grupo3.BookVerse.features.groups.groupMembershipRequest.controller;

import com.grupo3.BookVerse.features.groups.groupMembershipRequest.dto.GroupMembershipInvitationRequestDto;
import com.grupo3.BookVerse.features.groups.groupMembershipRequest.dto.GroupMembershipRequestResponseDto;
import com.grupo3.BookVerse.features.groups.groupMembershipRequest.service.GroupMembershipRequestService;
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
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(
        name = "Group Membership Requests",
        description = "Endpoints for managing reading group join requests and invitations"
)
public class GroupMembershipRequestController {

    private final GroupMembershipRequestService membershipRequestService;

    @PostMapping("/reading-groups/{groupId}/membership-requests")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Request to join a reading group",
            description = "Creates a pending join request for the authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Join request created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid membership request", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reading group not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Membership or pending request already exists", content = @Content)
    })
    public ResponseEntity<GroupMembershipRequestResponseDto> requestToJoin(
            @Parameter(
                    description = "External UUID of the reading group",
                    required = true
            )
            @PathVariable UUID groupId
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        membershipRequestService.requestToJoin(
                                groupId
                        )
                );
    }

    @PostMapping("/reading-groups/{groupId}/membership-invitations")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Invite a user to a reading group",
            description = "Creates a pending invitation for a user to join an active reading group.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Invitation created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid invitation", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reading group or user not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Membership or pending request already exists", content = @Content)
    })
    public ResponseEntity<GroupMembershipRequestResponseDto> inviteUser(
            @Parameter(
                    description = "External UUID of the reading group",
                    required = true
            )
            @PathVariable UUID groupId,
            @Valid @RequestBody GroupMembershipInvitationRequestDto requestDto
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        membershipRequestService.inviteUser(
                                groupId,
                                requestDto
                        )
                );
    }

    @PostMapping("/membership-requests/{requestId}/accept")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Accept a membership request",
            description = "Accepts a pending join request or invitation according to the request type and authenticated user's permissions.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Membership request accepted successfully"),
            @ApiResponse(responseCode = "400", description = "Request is not pending or cannot be processed", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Membership request not found", content = @Content)
    })
    public ResponseEntity<GroupMembershipRequestResponseDto> acceptRequest(
            @Parameter(
                    description = "External UUID of the membership request",
                    required = true
            )
            @PathVariable UUID requestId
    ) {
        return ResponseEntity.ok(
                membershipRequestService.acceptRequest(
                        requestId
                )
        );
    }

    @PostMapping("/membership-requests/{requestId}/reject")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Reject a membership request",
            description = "Rejects a pending join request or invitation according to the request type and authenticated user's permissions.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<GroupMembershipRequestResponseDto> rejectRequest(
            @PathVariable UUID requestId
    ) {
        return ResponseEntity.ok(
                membershipRequestService.rejectRequest(
                        requestId
                )
        );
    }

    @PostMapping("/membership-requests/{requestId}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Cancel a membership request",
            description = "Cancels a pending join request or invitation. Only the user who created it can cancel it.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<GroupMembershipRequestResponseDto> cancelRequest(
            @PathVariable UUID requestId
    ) {
        return ResponseEntity.ok(
                membershipRequestService.cancelRequest(
                        requestId
                )
        );
    }

    @GetMapping("/reading-groups/{groupId}/membership-requests/pending")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get pending join requests",
            description = "Retrieves pending join requests for a reading group. Available to the group creator, admins, and moderators.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<GroupMembershipRequestResponseDto>>
    getPendingJoinRequests(
            @PathVariable UUID groupId
    ) {
        return ResponseEntity.ok(
                membershipRequestService
                        .getPendingJoinRequests(groupId)
        );
    }

    @GetMapping("/membership-invitations/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get my pending group invitations",
            description = "Retrieves pending reading group invitations for the authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<GroupMembershipRequestResponseDto>>
    getMyPendingInvitations() {
        return ResponseEntity.ok(
                membershipRequestService
                        .getMyPendingInvitations()
        );
    }
}