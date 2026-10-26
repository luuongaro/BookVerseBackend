package com.grupo3.BookVerse.features.users.mappers;

import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.users.dto.UserRequestDto;
import com.grupo3.BookVerse.features.users.dto.UserResponseDto;
import com.grupo3.BookVerse.features.users.dto.UserUpdateRequestDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "idExternal", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "subscription", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "stories", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    @Mapping(target = "reviewReportsSubmitted", ignore = true)
    @Mapping(target = "reviewReportsModerated", ignore = true)
    @Mapping(target = "tipsSent", ignore = true)
    @Mapping(target = "tipsReceived", ignore = true)
    @Mapping(target = "groupsCreated", ignore = true)
    @Mapping(target = "groupMembers", ignore = true)
    @Mapping(target = "readingStatuses", ignore = true)
    UserEntity toEntity(UserRequestDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "idExternal", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "subscription", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "stories", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    @Mapping(target = "reviewReportsSubmitted", ignore = true)
    @Mapping(target = "reviewReportsModerated", ignore = true)
    @Mapping(target = "tipsSent", ignore = true)
    @Mapping(target = "tipsReceived", ignore = true)
    @Mapping(target = "groupsCreated", ignore = true)
    @Mapping(target = "groupMembers", ignore = true)
    @Mapping(target = "readingStatuses", ignore = true)
    void updateEntityFromDto(UserUpdateRequestDto dto, @MappingTarget UserEntity user);

    @Mapping(target = "username", expression = "java(user.getProfileUsername())")
    @Mapping(
            target = "status",
            expression = "java(user.getStatus() != null ? user.getStatus().name() : null)"
    )
    @Mapping(
            target = "roles",
            expression = "java(user.getRoles() == null ? java.util.Collections.emptyList() : user.getRoles().stream().map(role -> role.getName()).toList())"
    )
    UserResponseDto toResponseDto(UserEntity user);

    List<UserResponseDto> toResponseDtoList(List<UserEntity> users);
}