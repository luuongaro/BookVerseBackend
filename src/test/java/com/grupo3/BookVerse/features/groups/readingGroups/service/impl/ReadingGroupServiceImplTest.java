package com.grupo3.BookVerse.features.groups.readingGroups.service.impl;

import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberEntity;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberStatus;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberType;
import com.grupo3.BookVerse.features.groups.groupMember.repository.GroupMemberRepository;
import com.grupo3.BookVerse.features.groups.readingGroups.domain.ReadingGroupEntity;
import com.grupo3.BookVerse.features.groups.readingGroups.dto.ReadingGroupRequestDto;
import com.grupo3.BookVerse.features.groups.readingGroups.dto.ReadingGroupResponseDto;
import com.grupo3.BookVerse.features.groups.readingGroups.dto.UpdateReadingGroupRequestDto;
import com.grupo3.BookVerse.features.groups.readingGroups.mappers.ReadingGroupMapper;
import com.grupo3.BookVerse.features.groups.readingGroups.repository.ReadingGroupRepository;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadingGroupServiceImplTest {

    @Mock
    private ReadingGroupRepository repository;

    @Mock
    private ReadingGroupMapper mapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @InjectMocks
    private ReadingGroupServiceImpl service;

    private UUID groupId;
    private UserEntity authenticatedUser;

    @BeforeEach
    void setUp() {

        groupId = UUID.randomUUID();

        authenticatedUser = new UserEntity();
        authenticatedUser.setId(1L);
        authenticatedUser.setIdExternal(UUID.randomUUID());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        authenticatedUser,
                        null,
                        List.of()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createGroup_shouldCreateGroupAndCreatorMembership() {

        ReadingGroupRequestDto request =
                new ReadingGroupRequestDto(
                        "  Fantasy Readers  "
                );

        ReadingGroupEntity entity =
                new ReadingGroupEntity();

        ReadingGroupEntity saved =
                new ReadingGroupEntity();

        saved.setIdExternal(groupId);
        saved.setName("Fantasy Readers");
        saved.setCreatedBy(authenticatedUser);
        saved.setIsActive(true);

        ReadingGroupResponseDto response =
                mock(ReadingGroupResponseDto.class);

        when(repository.existsByNameIgnoreCaseAndIsActiveTrue(
                "Fantasy Readers"
        )).thenReturn(false);

        when(mapper.toEntity(request))
                .thenReturn(entity);

        when(repository.save(entity))
                .thenReturn(saved);

        when(mapper.toResponseDto(saved))
                .thenReturn(response);

        ReadingGroupResponseDto result =
                service.createGroup(request);

        assertEquals(response, result);

        assertEquals(
                "Fantasy Readers",
                entity.getName()
        );

        assertEquals(
                authenticatedUser,
                entity.getCreatedBy()
        );

        assertTrue(entity.getIsActive());

        ArgumentCaptor<GroupMemberEntity> membershipCaptor =
                ArgumentCaptor.forClass(
                        GroupMemberEntity.class
                );

        verify(groupMemberRepository)
                .save(membershipCaptor.capture());

        GroupMemberEntity creatorMembership =
                membershipCaptor.getValue();

        assertEquals(
                saved,
                creatorMembership.getGroup()
        );

        assertEquals(
                authenticatedUser,
                creatorMembership.getUser()
        );

        assertEquals(
                GroupMemberType.CREATOR,
                creatorMembership.getMemberType()
        );

        assertEquals(
                GroupMemberStatus.ACTIVE,
                creatorMembership.getStatus()
        );

        verify(repository).save(entity);
    }

    @Test
    void getAllGroups_shouldReturnActiveGroups() {

        ReadingGroupEntity activeGroup =
                new ReadingGroupEntity();

        activeGroup.setIdExternal(groupId);
        activeGroup.setName("Fantasy Readers");
        activeGroup.setIsActive(true);

        ReadingGroupResponseDto response =
                mock(ReadingGroupResponseDto.class);

        List<ReadingGroupEntity> groups =
                List.of(activeGroup);

        List<ReadingGroupResponseDto> responses =
                List.of(response);

        when(repository.findByIsActiveTrue())
                .thenReturn(groups);

        when(mapper.toResponseDtoList(groups))
                .thenReturn(responses);

        List<ReadingGroupResponseDto> result =
                service.getAllGroups();

        assertEquals(responses, result);

        verify(repository)
                .findByIsActiveTrue();
    }

    @Test
    void getGroupByIdExternal_shouldReturnActiveGroup() {

        ReadingGroupEntity entity =
                new ReadingGroupEntity();

        entity.setIdExternal(groupId);
        entity.setName("Fantasy Readers");
        entity.setIsActive(true);

        ReadingGroupResponseDto response =
                mock(ReadingGroupResponseDto.class);

        when(repository.findByIdExternal(groupId))
                .thenReturn(Optional.of(entity));

        when(mapper.toResponseDto(entity))
                .thenReturn(response);

        ReadingGroupResponseDto result =
                service.getGroupByIdExternal(groupId);

        assertEquals(response, result);

        verify(mapper)
                .toResponseDto(entity);
    }

    @Test
    void getGroupByIdExternal_shouldThrowWhenGroupDoesNotExist() {

        when(repository.findByIdExternal(groupId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.getGroupByIdExternal(
                        groupId
                )
        );

        verify(mapper, never())
                .toResponseDto(any());
    }

    @Test
    void updateGroup_shouldUpdateOnlyGroupName() {

        ReadingGroupEntity entity =
                new ReadingGroupEntity();

        entity.setIdExternal(groupId);
        entity.setName("Old Name");
        entity.setCreatedBy(authenticatedUser);
        entity.setIsActive(true);

        UpdateReadingGroupRequestDto request =
                new UpdateReadingGroupRequestDto(
                        "  Updated Group  "
                );

        ReadingGroupResponseDto response =
                mock(ReadingGroupResponseDto.class);

        when(repository.findByIdExternal(groupId))
                .thenReturn(Optional.of(entity));

        when(repository
                .existsByNameIgnoreCaseAndIsActiveTrueAndIdExternalNot(
                        "Updated Group",
                        groupId
                ))
                .thenReturn(false);

        when(repository.save(entity))
                .thenReturn(entity);

        when(mapper.toResponseDto(entity))
                .thenReturn(response);

        ReadingGroupResponseDto result =
                service.updateGroup(
                        groupId,
                        request
                );

        assertEquals(response, result);

        assertEquals(
                "Updated Group",
                entity.getName()
        );

        verify(repository)
                .save(entity);
    }

    @Test
    void deleteGroup_shouldDeactivateGroup() {

        ReadingGroupEntity entity =
                new ReadingGroupEntity();

        entity.setIdExternal(groupId);
        entity.setName("Fantasy Readers");
        entity.setCreatedBy(authenticatedUser);
        entity.setIsActive(true);

        when(repository.findByIdExternal(groupId))
                .thenReturn(Optional.of(entity));

        service.deleteGroup(groupId);

        assertFalse(entity.getIsActive());

        verify(repository)
                .save(entity);
    }
}