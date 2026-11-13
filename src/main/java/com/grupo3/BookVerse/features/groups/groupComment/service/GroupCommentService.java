package com.grupo3.BookVerse.features.groups.groupComment.service;

import com.grupo3.BookVerse.features.groups.groupComment.dto.GroupCommentRequestDto;
import com.grupo3.BookVerse.features.groups.groupComment.dto.GroupCommentResponseDto;
import com.grupo3.BookVerse.features.groups.groupComment.dto.GroupCommentUpdateRequestDto;
import com.grupo3.BookVerse.features.groups.groupComment.dto.GroupCommentsVisibilityResponseDto;

import java.util.List;
import java.util.UUID;

public interface GroupCommentService {

    GroupCommentResponseDto createComment(
            UUID groupId,
            GroupCommentRequestDto requestDto
    );

    GroupCommentsVisibilityResponseDto getVisibleCommentsByGroupId(
            UUID groupId
    );

    List<GroupCommentResponseDto> getActiveCommentsByUserId(
            UUID userId
    );

    GroupCommentResponseDto getCommentByIdExternal(
            UUID commentId
    );

    void deleteComment(
            UUID commentId
    );

    GroupCommentsVisibilityResponseDto getVisibleCommentsByCycleId(
            UUID groupId,
            UUID cycleId
    );

    GroupCommentResponseDto updateComment(
            UUID commentId,
            GroupCommentUpdateRequestDto requestDto
    );
}