package com.grupo3.BookVerse.features.reviewReport.mapper;

import com.grupo3.BookVerse.features.reviewReport.domain.ReviewReportEntity;
import com.grupo3.BookVerse.features.reviewReport.dtos.ReviewReportCreateRequestDto;
import com.grupo3.BookVerse.features.reviewReport.dtos.ReviewReportResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ReviewReportMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "idExternal", ignore = true)
    @Mapping(target = "review", ignore = true)
    @Mapping(target = "reporterUser", ignore = true)
    @Mapping(target = "moderatorUser", ignore = true)
    @Mapping(target = "resolutionComment", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    ReviewReportEntity toEntity(ReviewReportCreateRequestDto dto);

    @Mapping(source = "idExternal", target = "reportId")
    @Mapping(source = "review.idExternal", target = "reviewId")
    @Mapping(source = "review.book.idExternal", target = "bookId")
    @Mapping(source = "review.story.idExternal", target = "storyId")
    @Mapping(target = "workTitle", expression = "java(resolveWorkTitle(report))")
    @Mapping(target = "thumbnailUrl", expression = "java(resolveThumbnailUrl(report))")
    @Mapping(source = "review.reviewer.profileUsername", target = "reviewerName")
    @Mapping(source = "review.rating", target = "rating")
    @Mapping(source = "review.content", target = "reviewContent")
    @Mapping(source = "reporterUser.idExternal", target = "reporterUserId")
    @Mapping(source = "reporterUser.profileUsername", target = "reporterUsername")
    @Mapping(source = "moderatorUser.idExternal", target = "moderatorUserId")
    ReviewReportResponseDto toResponseDto(ReviewReportEntity report);

    List<ReviewReportResponseDto> toResponseListDto(List<ReviewReportEntity> reports);

    default String resolveWorkTitle(ReviewReportEntity report) {
        var review = report.getReview();

        if (review.getBook() != null) {
            return review.getBook().getTitle();
        }

        return review.getStory() != null
                ? review.getStory().getTitle()
                : null;
    }

    default String resolveThumbnailUrl(ReviewReportEntity report) {
        var review = report.getReview();

        return review.getBook() != null
                ? review.getBook().getThumbnailUrl()
                : null;
    }
}