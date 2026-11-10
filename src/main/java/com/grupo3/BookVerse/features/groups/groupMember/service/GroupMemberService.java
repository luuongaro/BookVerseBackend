package com.grupo3.BookVerse.features.groups.groupMember.service;

import com.grupo3.BookVerse.features.groups.groupMember.dto.GroupMemberRequestDto;
import com.grupo3.BookVerse.features.groups.groupMember.dto.GroupMemberResponseDto;

import java.util.List;
import java.util.UUID;

public interface GroupMemberService {

    GroupMemberResponseDto addMemberToGroup(UUID groupId, GroupMemberRequestDto requestDto);

    List<GroupMemberResponseDto> getActiveMembersByGroupId(UUID groupId);

    List<GroupMemberResponseDto> getActiveMembershipsByUserId(UUID userId);

    GroupMemberResponseDto getGroupMemberByIdExternal(UUID idExternal);

    void removeMemberFromGroup(UUID groupId, UUID userId);

    void leaveGroup(UUID groupId);
}
