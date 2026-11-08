package com.grupo3.BookVerse.features.status.mappers;


import com.grupo3.BookVerse.features.status.domain.ReadingStatusEntity;
import com.grupo3.BookVerse.features.status.dto.ReadingStatusRequestDto;

import com.grupo3.BookVerse.features.status.dto.ReadingStatusResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
@Mapper(componentModel = "spring")
public interface ReadingStatusMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "idExternal", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "book", ignore = true)
    @Mapping(target = "story", ignore = true)
    @Mapping(target = "shelfPosition", ignore = true)
    ReadingStatusEntity toEntity(ReadingStatusRequestDto dto);

    @Mapping(target = "userId", source = "user.idExternal")
    @Mapping(target = "bookId", source = "book.idExternal")
    @Mapping(target = "storyId", source = "story.idExternal")
    @Mapping(target = "progressType", source = "progressType")
    @Mapping(target = "progressValue", source = "progressValue")
    ReadingStatusResponseDto toResponseDto(ReadingStatusEntity entity);

    List<ReadingStatusResponseDto> toResponseDtoList(List<ReadingStatusEntity> entities);
}
