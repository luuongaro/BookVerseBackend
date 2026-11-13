package com.grupo3.BookVerse.features.groups.groupComment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupCommentsVisibilityResponseDto {

    private List<GroupCommentResponseDto> comments;

    private Integer totalActiveComments;

    private Integer visibleCount;

    private Integer hiddenByProgressCount;
}