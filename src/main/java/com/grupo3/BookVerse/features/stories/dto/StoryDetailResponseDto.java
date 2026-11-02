package com.grupo3.BookVerse.features.stories.dto;

import com.grupo3.BookVerse.features.chapters.dto.ChapterSummaryDto;
import com.grupo3.BookVerse.features.stories.domain.StoryAccessType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StoryDetailResponseDto {

    private UUID idExternal;
    private UUID authorId;
    private String authorUsername;
    private String title;
    private String description;
    private StoryAccessType accessType;
    private boolean hidden;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer chaptersCount;
    private Integer publishedChaptersCount;
    private List<ChapterSummaryDto> chapters;

}
