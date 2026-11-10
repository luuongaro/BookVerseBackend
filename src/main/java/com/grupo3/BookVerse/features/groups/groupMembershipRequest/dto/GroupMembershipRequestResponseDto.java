package com.grupo3.BookVerse.features.groups.groupMembershipRequest.dto;

import com.grupo3.BookVerse.features.groups.groupMembershipRequest.domain.GroupMembershipRequestStatus;
import com.grupo3.BookVerse.features.groups.groupMembershipRequest.domain.GroupMembershipRequestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMembershipRequestResponseDto {

    private UUID idExternal;

    private UUID groupId;

    private UUID targetUserId;

    private UUID createdById;

    private GroupMembershipRequestType type;

    private GroupMembershipRequestStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime respondedAt;
}