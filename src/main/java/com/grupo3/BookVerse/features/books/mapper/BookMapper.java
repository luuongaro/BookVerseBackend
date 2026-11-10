package com.grupo3.BookVerse.features.books.mapper;

import com.grupo3.BookVerse.features.books.domain.BookEntity;
import com.grupo3.BookVerse.features.books.dto.BookResponseDto;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleEntity;
import com.grupo3.BookVerse.features.status.domain.ReadingStatusEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Objects;

@Mapper(componentModel = "spring")
public interface BookMapper {

    List<BookResponseDto> toResponseDtoList(List<BookEntity> bookEntities);

    @Mapping(
            target = "readingGroupsCount",
            expression = "java(mapReadingGroupsCount(bookEntity.getReadingCycles()))"
    )
    @Mapping(
            target = "readingStatusesCount",
            expression = "java(mapReadingStatusesCount(bookEntity.getReadingStatuses()))"
    )
    BookResponseDto toResponseDto(BookEntity bookEntity);

    default Integer mapReadingGroupsCount(List<GroupReadingCycleEntity> readingCycles) {

        if (readingCycles == null) {
            return 0;
        }

        return (int) readingCycles.stream()
                .map(GroupReadingCycleEntity::getGroup)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    default Integer mapReadingStatusesCount(List<ReadingStatusEntity> readingStatuses) {
        return readingStatuses != null ? readingStatuses.size() : 0;
    }
}