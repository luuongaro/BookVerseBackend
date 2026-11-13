package com.grupo3.BookVerse.features.groups.groupComment.dto;

import com.grupo3.BookVerse.features.status.domain.ProgressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupCommentRequestDto {

    @NotBlank(message = "Content is required")
    private String content;

    @NotNull(message = "Progress type is required")
    private ProgressType progressType;

    @NotNull(message = "Progress value is required")
    @Positive(message = "Progress value must be greater than 0")
    private Integer progressValue;
}