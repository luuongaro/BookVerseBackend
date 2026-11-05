package com.grupo3.BookVerse.features.groups.readingGroups.mappers;

import com.grupo3.BookVerse.features.groups.readingGroups.domain.ReadingGroupEntity;
import com.grupo3.BookVerse.features.groups.readingGroups.dto.ReadingGroupRequestDto;
import com.grupo3.BookVerse.features.groups.readingGroups.dto.ReadingGroupResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReadingGroupMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "idExternal", ignore = true)
    @Mapping(target = "book", ignore = true)
    @Mapping(target = "story", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "members", ignore = true)
    @Mapping(target = "goals", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "totalChapters", ignore = true)
    ReadingGroupEntity toEntity(ReadingGroupRequestDto dto);

    @Mapping(target = "bookId", source = "book.idExternal")
    @Mapping(target = "storyId", source = "story.idExternal")
    @Mapping(target = "createdByUserId", source = "createdBy.idExternal")
    ReadingGroupResponseDto toResponseDto(ReadingGroupEntity entity);

    List<ReadingGroupResponseDto> toResponseDtoList(List<ReadingGroupEntity> entities);
}
