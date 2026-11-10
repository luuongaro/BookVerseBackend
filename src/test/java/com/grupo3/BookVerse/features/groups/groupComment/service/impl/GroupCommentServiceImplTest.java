package com.grupo3.BookVerse.features.groups.groupComment.service.impl;

import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.books.domain.BookEntity;
import com.grupo3.BookVerse.features.groups.groupComment.domain.GroupCommentEntity;
import com.grupo3.BookVerse.features.groups.groupComment.domain.GroupCommentStatus;
import com.grupo3.BookVerse.features.groups.groupComment.dto.GroupCommentRequestDto;
import com.grupo3.BookVerse.features.groups.groupComment.dto.GroupCommentResponseDto;
import com.grupo3.BookVerse.features.groups.groupComment.mappers.GroupCommentMapper;
import com.grupo3.BookVerse.features.groups.groupComment.repository.GroupCommentRepository;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberStatus;
import com.grupo3.BookVerse.features.groups.groupMember.repository.GroupMemberRepository;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleEntity;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleStatus;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.repository.GroupReadingCycleRepository;
import com.grupo3.BookVerse.features.groups.readingGroups.domain.ReadingGroupEntity;
import com.grupo3.BookVerse.features.groups.readingGroups.repository.ReadingGroupRepository;
import com.grupo3.BookVerse.features.roles.domain.RoleEntity;
import com.grupo3.BookVerse.features.status.domain.ProgressType;
import com.grupo3.BookVerse.features.status.domain.ReadingStatusEntity;
import com.grupo3.BookVerse.features.status.repository.ReadingStatusRepository;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupCommentServiceImplTest {

    @Mock
    private GroupCommentRepository groupCommentRepository;

    @Mock
    private GroupCommentMapper groupCommentMapper;

    @Mock
    private ReadingGroupRepository readingGroupRepository;

    @Mock
    private GroupReadingCycleRepository groupReadingCycleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private ReadingStatusRepository readingStatusRepository;

    @InjectMocks
    private GroupCommentServiceImpl groupCommentService;

    private UUID groupId;
    private UUID userId;
    private UUID bookId;
    private UserEntity user;
    private ReadingGroupEntity group;
    private ReadingStatusEntity readingStatus;
    private BookEntity book;
    private GroupReadingCycleEntity activeCycle;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        userId = UUID.randomUUID();
        bookId = UUID.randomUUID();

        user = new UserEntity();
        user.setId(1L);
        user.setIdExternal(userId);

        book = new BookEntity();
        book.setIdExternal(bookId);

        group = new ReadingGroupEntity();
        group.setIdExternal(groupId);

        activeCycle = new GroupReadingCycleEntity();
        activeCycle.setGroup(group);
        activeCycle.setBook(book);
        activeCycle.setStory(null);
        activeCycle.setStatus(GroupReadingCycleStatus.ACTIVE);

        readingStatus = new ReadingStatusEntity();
        readingStatus.setProgressType(ProgressType.PERCENTAGE);
        readingStatus.setProgressValue(60);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        List.of()
                )
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createComment_ShouldSaveCommentWhenUserCanAccessProgress() {
        GroupCommentRequestDto requestDto = GroupCommentRequestDto.builder()
                .content("Nuevo comentario")
                .progressPercent(50)
                .build();

        GroupCommentEntity entity = new GroupCommentEntity();
        entity.setContent("Nuevo comentario");

        GroupCommentResponseDto dto = GroupCommentResponseDto.builder()
                .content("Nuevo comentario")
                .progressPercent(50)
                .build();

        when(readingGroupRepository.findByIdExternal(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupIdExternalAndUserIdExternalAndStatus(
                groupId,
                userId,
                GroupMemberStatus.ACTIVE
        )).thenReturn(true);
        when(readingStatusRepository.findByUserIdExternalAndBookIdExternal(userId, bookId))
                .thenReturn(Optional.of(readingStatus));
        when(groupCommentMapper.toEntity(requestDto)).thenReturn(entity);
        when(groupCommentRepository.save(entity)).thenReturn(entity);
        when(groupCommentMapper.toResponseDto(entity)).thenReturn(dto);

        GroupCommentResponseDto result = groupCommentService.createComment(groupId, requestDto);

        assertNotNull(result);
        assertEquals("Nuevo comentario", result.getContent());
        assertEquals(GroupCommentStatus.ACTIVE, entity.getStatus());
        assertEquals(group, entity.getGroup());
        assertEquals(user, entity.getUser());

        verify(groupCommentRepository).save(entity);
    }

    @Test
    void getVisibleCommentsByGroupId_ShouldReturnCommentsUpToUserProgress() {
        GroupCommentEntity entity = GroupCommentEntity.builder()
                .content("Comentario visible")
                .progressPercent(40)
                .status(GroupCommentStatus.ACTIVE)
                .build();

        GroupCommentResponseDto dto = GroupCommentResponseDto.builder()
                .content("Comentario visible")
                .progressPercent(40)
                .build();

        when(readingGroupRepository.findByIdExternal(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupIdExternalAndUserIdExternalAndStatus(
                groupId,
                userId,
                GroupMemberStatus.ACTIVE
        )).thenReturn(true);
        when(readingStatusRepository.findByUserIdExternalAndBookIdExternal(userId, bookId))
                .thenReturn(Optional.of(readingStatus));
        when(groupCommentRepository.findByGroupIdExternalAndStatusAndProgressPercentLessThanEqualOrderByCreatedAtAsc(
                groupId,
                GroupCommentStatus.ACTIVE,
                60
        )).thenReturn(List.of(entity));
        when(groupCommentMapper.toResponseDtoList(List.of(entity))).thenReturn(List.of(dto));

        List<GroupCommentResponseDto> result = groupCommentService.getVisibleCommentsByGroupId(groupId);

        assertEquals(1, result.size());
        assertEquals("Comentario visible", result.get(0).getContent());
    }

    @Test
    void getCommentByIdExternal_ShouldReturnCommentForAuthor() {
        UUID commentId = UUID.randomUUID();

        GroupCommentEntity entity = GroupCommentEntity.builder()
                .idExternal(commentId)
                .group(group)
                .user(user)
                .content("Comentario prueba")
                .progressPercent(20)
                .status(GroupCommentStatus.ACTIVE)
                .build();

        GroupCommentResponseDto dto = GroupCommentResponseDto.builder()
                .content("Comentario prueba")
                .build();

        when(groupCommentRepository.findByIdExternal(commentId)).thenReturn(Optional.of(entity));
        when(groupCommentMapper.toResponseDto(entity)).thenReturn(dto);

        GroupCommentResponseDto result = groupCommentService.getCommentByIdExternal(commentId);

        assertNotNull(result);
        assertEquals("Comentario prueba", result.getContent());
    }

    @Test
    void getCommentByIdExternal_ShouldThrowWhenNotFound() {
        UUID commentId = UUID.randomUUID();

        when(groupCommentRepository.findByIdExternal(commentId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> groupCommentService.getCommentByIdExternal(commentId)
        );
    }

    @Test
    void getActiveCommentsByUserId_ShouldReturnOwnComments() {
        UUID commentAuthorId = UUID.randomUUID();
        UserEntity commentAuthor = new UserEntity();
        commentAuthor.setId(2L);
        commentAuthor.setIdExternal(commentAuthorId);
        commentAuthor.getRoles().add(RoleEntity.builder().name("ROLE_ADMIN").build());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(commentAuthor, null, List.of()));
        SecurityContextHolder.setContext(context);

        GroupCommentEntity entity = GroupCommentEntity.builder()
                .user(commentAuthor)
                .content("Mi comentario")
                .status(GroupCommentStatus.ACTIVE)
                .build();

        GroupCommentResponseDto dto = GroupCommentResponseDto.builder()
                .content("Mi comentario")
                .build();

        when(userRepository.findByIdExternal(commentAuthorId)).thenReturn(Optional.of(commentAuthor));
        when(groupCommentRepository.findByUserIdExternalAndStatusOrderByCreatedAtDesc(
                commentAuthorId,
                GroupCommentStatus.ACTIVE
        )).thenReturn(List.of(entity));
        when(groupCommentMapper.toResponseDtoList(List.of(entity))).thenReturn(List.of(dto));

        List<GroupCommentResponseDto> result = groupCommentService.getActiveCommentsByUserId(commentAuthorId);

        assertEquals(1, result.size());
        assertEquals("Mi comentario", result.get(0).getContent());
    }

    @Test
    void deleteComment_ShouldMarkAsDeletedWhenAuthorDeletes() {
        UUID commentId = UUID.randomUUID();

        GroupCommentEntity entity = GroupCommentEntity.builder()
                .idExternal(commentId)
                .group(group)
                .user(user)
                .status(GroupCommentStatus.ACTIVE)
                .build();

        when(groupCommentRepository.findByIdExternal(commentId)).thenReturn(Optional.of(entity));

        groupCommentService.deleteComment(commentId);

        assertEquals(GroupCommentStatus.DELETED, entity.getStatus());
        verify(groupCommentRepository).save(entity);
    }
}
