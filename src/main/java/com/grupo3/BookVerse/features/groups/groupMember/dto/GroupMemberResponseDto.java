package com.grupo3.BookVerse.features.groups.groupMember.dto;

import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberStatus;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMemberResponseDto {

    private UUID idExternal;

    private UUID groupId;
    private String groupName;

    private UUID userId;
    private String userName;

    private GroupMemberType memberType;
    private GroupMemberStatus status;

    private LocalDateTime joinedAt;
}