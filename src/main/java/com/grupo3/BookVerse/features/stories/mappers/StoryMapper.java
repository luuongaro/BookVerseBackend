package com.grupo3.BookVerse.features.stories.mappers;

import com.grupo3.BookVerse.features.chapters.domain.ChapterEntity;
import com.grupo3.BookVerse.features.stories.domain.StoryEntity;
import com.grupo3.BookVerse.features.stories.dto.StoryRequestDto;
import com.grupo3.BookVerse.features.stories.dto.StoryResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "idExternal", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "chapters", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "hidden", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    StoryEntity toEntity(StoryRequestDto dto);

    @Mapping(target = "authorId", source = "author.idExternal")
    @Mapping(target = "authorUsername", expression = "java(storyEntity.getAuthor() == null ? null : storyEntity.getAuthor().getProfileUsername())")
    @Mapping(target = "chaptersCount", expression = "java(mapChaptersCount(storyEntity.getChapters()))")
    StoryResponseDto toResponseDto(StoryEntity storyEntity);

    List<StoryResponseDto> toResponseDtoList(List<StoryEntity> stories);

    default Integer mapChaptersCount(List<ChapterEntity> chapters) {
        return chapters != null ? chapters.size() : 0;
    }
}
