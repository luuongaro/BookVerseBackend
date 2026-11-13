package com.grupo3.BookVerse.features.groups.groupComment.dto;

import com.grupo3.BookVerse.features.status.domain.ProgressType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupCommentResponseDto {

    private UUID idExternal;

    private UUID groupId;

    private UUID readingCycleId;

    private UUID userId;

    private String userName;

    private String content;

    private ProgressType progressType;

    private Integer progressValue;

    private LocalDateTime createdAt;
}