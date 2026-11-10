package com.grupo3.BookVerse.features.groups.groupProgress.service.impl;

import com.grupo3.BookVerse.features.books.domain.BookEntity;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberEntity;
import com.grupo3.BookVerse.features.groups.groupProgress.service.GroupProgressService;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleEntity;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleStatus;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.repository.GroupReadingCycleRepository;
import com.grupo3.BookVerse.features.groups.groupReadingParticipant.domain.GroupReadingParticipantEntity;
import com.grupo3.BookVerse.features.groups.groupReadingParticipant.domain.GroupReadingParticipantStatus;
import com.grupo3.BookVerse.features.groups.groupReadingParticipant.repository.GroupReadingParticipantRepository;
import com.grupo3.BookVerse.features.groups.readingGroups.domain.ReadingGroupEntity;
import com.grupo3.BookVerse.features.groups.readingGroups.repository.ReadingGroupRepository;
import com.grupo3.BookVerse.features.groups.groupGoals.repository.GroupGoalsRepository;
import com.grupo3.BookVerse.features.status.domain.ProgressType;
import com.grupo3.BookVerse.features.status.domain.ReadingStatusEntity;
import com.grupo3.BookVerse.features.status.domain.ReadingStatusEnum;
import com.grupo3.BookVerse.features.status.repository.ReadingStatusRepository;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupProgressServiceImplTest {

    @Mock
    private ReadingGroupRepository readingGroupRepository;

    @Mock
    private GroupGoalsRepository groupGoalsRepository;

    @Mock
    private ReadingStatusRepository readingStatusRepository;

    @Mock
    private GroupReadingCycleRepository groupReadingCycleRepository;

    @Mock
    private GroupReadingParticipantRepository participantRepository;

    @InjectMocks
    private GroupProgressServiceImpl groupProgressService;

    private UUID groupId;
    private UUID cycleId;
    private UUID bookId;

    private ReadingGroupEntity group;
    private GroupReadingCycleEntity cycle;
    private BookEntity book;

    @BeforeEach
    void setUp() {

        groupId = UUID.randomUUID();
        cycleId = UUID.randomUUID();
        bookId = UUID.randomUUID();

        group = new ReadingGroupEntity();
        group.setIdExternal(groupId);
        group.setIsActive(true);

        book = new BookEntity();
        book.setIdExternal(bookId);

        cycle = new GroupReadingCycleEntity();
        cycle.setIdExternal(cycleId);
        cycle.setGroup(group);
        cycle.setBook(book);
        cycle.setStatus(GroupReadingCycleStatus.ACTIVE);
        cycle.setTotalChapters(100);
    }

    @Test
    void calculateAveragePercentage_shouldOnlyIncludeCycleParticipants() {

        UserEntity participantUser1 = new UserEntity();
        participantUser1.setIdExternal(UUID.randomUUID());

        UserEntity participantUser2 = new UserEntity();
        participantUser2.setIdExternal(UUID.randomUUID());

        UserEntity nonParticipantUser = new UserEntity();
        nonParticipantUser.setIdExternal(UUID.randomUUID());

        GroupMemberEntity member1 = new GroupMemberEntity();
        member1.setGroup(group);
        member1.setUser(participantUser1);

        GroupMemberEntity member2 = new GroupMemberEntity();
        member2.setGroup(group);
        member2.setUser(participantUser2);

        GroupMemberEntity nonParticipantMember = new GroupMemberEntity();
        nonParticipantMember.setGroup(group);
        nonParticipantMember.setUser(nonParticipantUser);

        group.setMembers(List.of(
                member1,
                member2,
                nonParticipantMember
        ));

        GroupReadingParticipantEntity participation1 =
                new GroupReadingParticipantEntity();

        participation1.setReadingCycle(cycle);
        participation1.setGroupMember(member1);
        participation1.setStatus(
                GroupReadingParticipantStatus.ACTIVE
        );

        GroupReadingParticipantEntity participation2 =
                new GroupReadingParticipantEntity();

        participation2.setReadingCycle(cycle);
        participation2.setGroupMember(member2);
        participation2.setStatus(
                GroupReadingParticipantStatus.COMPLETED
        );

        ReadingStatusEntity status1 =
                new ReadingStatusEntity();

        status1.setUser(participantUser1);
        status1.setBook(book);
        status1.setStatus(
                ReadingStatusEnum.IN_PROGRESS
        );
        status1.setProgressType(
                ProgressType.PERCENTAGE
        );
        status1.setProgressValue(50);

        ReadingStatusEntity status2 =
                new ReadingStatusEntity();

        status2.setUser(participantUser2);
        status2.setBook(book);
        status2.setStatus(
                ReadingStatusEnum.FINISHED
        );
        status2.setProgressType(
                ProgressType.PERCENTAGE
        );
        status2.setProgressValue(100);

        when(readingGroupRepository.findByIdExternal(groupId))
                .thenReturn(Optional.of(group));

        when(groupReadingCycleRepository
                .findByIdExternalAndGroup_IdExternal(
                        cycleId,
                        groupId
                ))
                .thenReturn(Optional.of(cycle));

        when(participantRepository
                .findByReadingCycle_IdExternalAndStatusIn(
                        cycleId,
                        List.of(
                                GroupReadingParticipantStatus.ACTIVE,
                                GroupReadingParticipantStatus.COMPLETED
                        )
                ))
                .thenReturn(
                        List.of(
                                participation1,
                                participation2
                        )
                );

        when(readingStatusRepository
                .findByUserIdExternalInAndBookIdExternal(
                        List.of(
                                participantUser1.getIdExternal(),
                                participantUser2.getIdExternal()
                        ),
                        bookId
                ))
                .thenReturn(
                        List.of(
                                status1,
                                status2
                        )
                );

        double result =
                groupProgressService.calculateAveragePercentage(
                        groupId,
                        cycleId
                );

        assertEquals(
                75.0,
                result,
                0.001
        );
    }
}