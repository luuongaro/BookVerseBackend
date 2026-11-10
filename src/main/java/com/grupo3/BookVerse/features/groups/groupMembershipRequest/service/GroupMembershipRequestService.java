package com.grupo3.BookVerse.features.groups.groupMembershipRequest.service;

import com.grupo3.BookVerse.features.groups.groupMembershipRequest.dto.GroupMembershipInvitationRequestDto;
import com.grupo3.BookVerse.features.groups.groupMembershipRequest.dto.GroupMembershipRequestResponseDto;

import java.util.List;
import java.util.UUID;

public interface GroupMembershipRequestService {

    GroupMembershipRequestResponseDto requestToJoin(
            UUID groupId
    );

    GroupMembershipRequestResponseDto inviteUser(
            UUID groupId,
            GroupMembershipInvitationRequestDto requestDto
    );

    GroupMembershipRequestResponseDto acceptRequest(
            UUID requestId
    );

    GroupMembershipRequestResponseDto rejectRequest(
            UUID requestId
    );

    GroupMembershipRequestResponseDto cancelRequest(
            UUID requestId
    );

    List<GroupMembershipRequestResponseDto> getPendingJoinRequests(
            UUID groupId
    );

    List<GroupMembershipRequestResponseDto> getMyPendingInvitations();
}