package com.grupo3.BookVerse.features.stories.dto;

import com.grupo3.BookVerse.features.stories.domain.StoryAccessType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StoryRequestDto {

    @NotBlank(message = "Story title is required")
    @Size(max = 150, message = "Story title must not exceed 150 characters")
    private String title;

    @Size(max = 80, message = "Genre must not exceed 80 characters")
    private String genre;

    @NotBlank(message = "Story description is required")
    @Size(max = 1000, message = "Story description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Story access type is required")
    private StoryAccessType accessType;
}
