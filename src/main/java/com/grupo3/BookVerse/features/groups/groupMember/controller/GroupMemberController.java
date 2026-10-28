package com.grupo3.BookVerse.features.groups.groupMember.controller;

import com.grupo3.BookVerse.features.groups.groupMember.dto.GroupMemberRequestDto;
import com.grupo3.BookVerse.features.groups.groupMember.dto.GroupMemberResponseDto;
import com.grupo3.BookVerse.features.groups.groupMember.service.GroupMemberService;
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
@RequestMapping("/api/group-members")
@RequiredArgsConstructor
@Tag(
        name = "Group Members",
        description = "Endpoints for managing reading group memberships"
)
public class GroupMemberController {

    private final GroupMemberService groupMemberService;

    @PostMapping("/group/{groupId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Add a user to a reading group",
            description = "Adds a user to a reading group using the target user's email address.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Group member added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Reading group or user not found"),
            @ApiResponse(responseCode = "409", description = "User is already an active member of the group")
    })
    public ResponseEntity<GroupMemberResponseDto> addMemberToGroup(
            @Parameter(
                    description = "External UUID of the reading group",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID groupId,
            @Valid @RequestBody GroupMemberRequestDto requestDto
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(groupMemberService.addMemberToGroup(groupId, requestDto));
    }

    @GetMapping("/{idExternal}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get group member by external id",
            description = "Retrieves a specific group membership by its external UUID.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Group member retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Group member not found", content = @Content)
    })
    public ResponseEntity<GroupMemberResponseDto> getGroupMemberByIdExternal(
            @Parameter(
                    description = "External UUID of the group membership",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID idExternal
    ) {
        return ResponseEntity.ok(
                groupMemberService.getGroupMemberByIdExternal(idExternal)
        );
    }

    @GetMapping("/group/{groupId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get active members by reading group",
            description = "Retrieves all active members of a specific reading group using the group's external UUID.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Group members retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reading group not found", content = @Content)
    })
    public ResponseEntity<List<GroupMemberResponseDto>> getActiveMembersByGroupId(
            @Parameter(
                    description = "External UUID of the reading group",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID groupId
    ) {
        return ResponseEntity.ok(
                groupMemberService.getActiveMembersByGroupId(groupId)
        );
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get active group memberships by user",
            description = "Retrieves all active group memberships associated with a specific user using the user's external UUID.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User group memberships retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<List<GroupMemberResponseDto>> getActiveMembershipsByUserId(
            @Parameter(
                    description = "External UUID of the user",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(
                groupMemberService.getActiveMembershipsByUserId(userId)
        );
    }

    @DeleteMapping("/group/{groupId}/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Remove a member from a reading group",
            description = "Removes a user from a reading group using the group's external UUID and the user's external UUID.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Group member removed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reading group, user, or membership not found", content = @Content)
    })
    public ResponseEntity<Void> removeMemberFromGroup(
            @Parameter(
                    description = "External UUID of the reading group",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID groupId,
            @Parameter(
                    description = "External UUID of the user to remove from the group",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID userId
    ) {
        groupMemberService.removeMemberFromGroup(groupId, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/group/{groupId}/leave")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Leave a reading group",
            description = "Allows the authenticated user to leave a reading group.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully left the group"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @ApiResponse(responseCode = "404", description = "Reading group or membership not found", content = @Content)
    })
    public ResponseEntity<Void> leaveGroup(
            @Parameter(
                    description = "External UUID of the reading group",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID groupId
    ) {
        groupMemberService.leaveGroup(groupId);
        return ResponseEntity.noContent().build();
    }
}

