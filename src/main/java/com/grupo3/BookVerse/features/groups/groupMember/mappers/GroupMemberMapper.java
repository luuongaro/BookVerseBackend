package com.grupo3.BookVerse.features.groups.groupMember.mappers;

import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberEntity;
import com.grupo3.BookVerse.features.groups.groupMember.dto.GroupMemberResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GroupMemberMapper {

    @Mapping(target = "groupId", source = "group.idExternal")
    @Mapping(target = "groupName", source = "group.name")
    @Mapping(target = "userId", source = "user.idExternal")
    @Mapping(target = "userName", source = "user.username")
    GroupMemberResponseDto toResponseDto(GroupMemberEntity entity);

    List<GroupMemberResponseDto> toResponseDtoList(List<GroupMemberEntity> entities);
}