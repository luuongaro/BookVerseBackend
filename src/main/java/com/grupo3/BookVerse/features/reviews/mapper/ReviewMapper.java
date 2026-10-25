package com.grupo3.BookVerse.features.reviews.mapper;

import com.grupo3.BookVerse.features.reviews.domain.ReviewEntity;
import com.grupo3.BookVerse.features.reviews.dto.ReviewCreateRequestDto;
import com.grupo3.BookVerse.features.reviews.dto.ReviewResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ReviewMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "idExternal", ignore = true)
    @Mapping(target = "reviewer", ignore = true)
    @Mapping(target = "book", ignore = true)
    @Mapping(target = "story", ignore = true)
    @Mapping(target = "reports", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "takedownAt", ignore = true)
    @Mapping(target = "takedownReason", ignore = true)
    @Mapping(target = "takedownStatus", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ReviewEntity toEntity(ReviewCreateRequestDto dto);

    @Mapping(source = "idExternal", target = "reviewId")
    @Mapping(source = "reviewer.idExternal", target = "reviewerId")
    @Mapping(source = "reviewer.profileUsername", target = "reviewerName")
    @Mapping(source = "book.idExternal", target = "bookId")
    @Mapping(source = "story.idExternal", target = "storyId")
    ReviewResponseDto toResponseDto(ReviewEntity review);

    List<ReviewResponseDto> toResponseListDto(List<ReviewEntity> reviews);
}
