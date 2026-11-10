package com.grupo3.BookVerse.features.groups.groupGoals.service.impl;

import com.grupo3.BookVerse.features.groups.groupGoals.domain.GoalStatus;
import com.grupo3.BookVerse.features.groups.groupGoals.domain.GroupGoalsEntity;
import com.grupo3.BookVerse.features.groups.groupGoals.dto.GroupGoalsResponseDto;
import com.grupo3.BookVerse.features.groups.groupGoals.mappers.GroupGoalsMapper;
import com.grupo3.BookVerse.features.groups.groupGoals.repository.GroupGoalsRepository;
import com.grupo3.BookVerse.features.groups.groupMember.repository.GroupMemberRepository;
import com.grupo3.BookVerse.features.groups.groupProgress.service.GroupProgressService;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleEntity;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleStatus;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.repository.GroupReadingCycleRepository;
import com.grupo3.BookVerse.features.groups.readingGroups.domain.ReadingGroupEntity;
import com.grupo3.BookVerse.features.groups.readingGroups.repository.ReadingGroupRepository;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupGoalsServiceImplTest {

    @Mock
    private GroupGoalsRepository groupGoalsRepository;

    @Mock
    private GroupGoalsMapper groupGoalsMapper;

    @Mock
    private ReadingGroupRepository readingGroupRepository;

    @Mock
    private GroupReadingCycleRepository groupReadingCycleRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private GroupProgressService groupProgressService;

    @InjectMocks
    private GroupGoalsServiceImpl groupGoalsService;

    private UUID groupId;
    private UUID cycleId;

    private ReadingGroupEntity group;
    private GroupReadingCycleEntity cycle;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        cycleId = UUID.randomUUID();

        user = new UserEntity();
        user.setId(1L);
        user.setIdExternal(UUID.randomUUID());

        group = new ReadingGroupEntity();
        group.setIdExternal(groupId);
        group.setIsActive(true);
        group.setCreatedBy(user);

        cycle = new GroupReadingCycleEntity();
        cycle.setIdExternal(cycleId);
        cycle.setGroup(group);
        cycle.setStatus(GroupReadingCycleStatus.ACTIVE);

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
    void getGoalsByCycleId_shouldReturnGoals() {

        GroupGoalsEntity entity = new GroupGoalsEntity();

        GroupGoalsResponseDto response =
                mock(GroupGoalsResponseDto.class);

        when(readingGroupRepository.findByIdExternal(groupId))
                .thenReturn(Optional.of(group));

        when(groupReadingCycleRepository
                .findByIdExternalAndGroup_IdExternal(
                        cycleId,
                        groupId
                ))
                .thenReturn(Optional.of(cycle));

        when(groupGoalsRepository
                .findByReadingCycle_IdExternalOrderByUpdatedAtDesc(
                        cycleId
                ))
                .thenReturn(List.of(entity));

        when(groupGoalsMapper.toResponseDtoList(List.of(entity)))
                .thenReturn(List.of(response));

        List<GroupGoalsResponseDto> result =
                groupGoalsService.getGoalsByCycleId(
                        groupId,
                        cycleId
                );

        assertEquals(1, result.size());
        assertEquals(response, result.get(0));

        verify(groupGoalsRepository)
                .findByReadingCycle_IdExternalOrderByUpdatedAtDesc(
                        cycleId
                );
    }

    @Test
    void getActiveGoalByCycleId_shouldReturnActiveGoal() {

        GroupGoalsEntity entity = new GroupGoalsEntity();
        entity.setStatus(GoalStatus.ACTIVE);

        GroupGoalsResponseDto response =
                mock(GroupGoalsResponseDto.class);

        when(readingGroupRepository.findByIdExternal(groupId))
                .thenReturn(Optional.of(group));

        when(groupReadingCycleRepository
                .findByIdExternalAndGroup_IdExternal(
                        cycleId,
                        groupId
                ))
                .thenReturn(Optional.of(cycle));

        when(groupGoalsRepository
                .findByReadingCycle_IdExternalAndStatus(
                        cycleId,
                        GoalStatus.ACTIVE
                ))
                .thenReturn(Optional.of(entity));

        when(groupGoalsMapper.toResponseDto(entity))
                .thenReturn(response);

        GroupGoalsResponseDto result =
                groupGoalsService.getActiveGoalByCycleId(
                        groupId,
                        cycleId
                );

        assertEquals(response, result);
    }
}