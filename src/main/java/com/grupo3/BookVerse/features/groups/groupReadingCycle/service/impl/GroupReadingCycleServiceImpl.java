package com.grupo3.BookVerse.features.groups.groupReadingCycle.service.impl;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.books.domain.BookEntity;
import com.grupo3.BookVerse.features.books.repository.BookRepository;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberStatus;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberType;
import com.grupo3.BookVerse.features.groups.groupMember.repository.GroupMemberRepository;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleEntity;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.domain.GroupReadingCycleStatus;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.dto.GroupReadingCycleRequestDto;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.dto.GroupReadingCycleResponseDto;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.dto.UpdateGroupReadingCycleRequestDto;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.mappers.GroupReadingCycleMapper;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.repository.GroupReadingCycleRepository;
import com.grupo3.BookVerse.features.groups.groupReadingCycle.service.GroupReadingCycleService;
import com.grupo3.BookVerse.features.groups.readingGroups.domain.ReadingGroupEntity;
import com.grupo3.BookVerse.features.groups.readingGroups.repository.ReadingGroupRepository;
import com.grupo3.BookVerse.features.stories.domain.StoryEntity;
import com.grupo3.BookVerse.features.stories.repository.StoryRepository;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupReadingCycleServiceImpl implements GroupReadingCycleService {

    private final GroupReadingCycleRepository cycleRepository;
    private final GroupReadingCycleMapper cycleMapper;

    private final ReadingGroupRepository readingGroupRepository;
    private final BookRepository bookRepository;
    private final StoryRepository storyRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Override
    @Transactional
    public GroupReadingCycleResponseDto createCycle(
            UUID groupId,
            GroupReadingCycleRequestDto dto
    ) {
        ReadingGroupEntity group = findGroup(groupId);
        validateGroupIsActive(group);

        UserEntity authenticatedUser = getAuthenticatedUser();
        validateCanManageCycle(group, authenticatedUser);

        validateContent(dto.bookId(), dto.storyId());
        validateCycleReference(dto);

        BookEntity book = null;
        StoryEntity story = null;

        if (dto.bookId() != null) {
            book = findBook(dto.bookId());
        }

        if (dto.storyId() != null) {
            story = findStory(dto.storyId());
        }

        GroupReadingCycleEntity entity = cycleMapper.toEntity(dto);

        entity.setGroup(group);
        entity.setBook(book);
        entity.setStory(story);
        entity.setStatus(GroupReadingCycleStatus.PLANNED);

        if (dto.editionReference() != null) {
            entity.setEditionReference(dto.editionReference().trim());
        }

        GroupReadingCycleEntity saved = cycleRepository.save(entity);

        return cycleMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupReadingCycleResponseDto getCycleByIdExternal(
            UUID groupId,
            UUID cycleId
    ) {
        GroupReadingCycleEntity cycle = findCycle(groupId, cycleId);

        validateGroupIsActive(cycle.getGroup());

        return cycleMapper.toResponseDto(cycle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupReadingCycleResponseDto> getCyclesByGroupId(UUID groupId) {
        ReadingGroupEntity group = findGroup(groupId);
        validateGroupIsActive(group);

        return cycleMapper.toResponseDtoList(
                cycleRepository.findByGroup_IdExternalOrderByCreatedAtDesc(groupId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public GroupReadingCycleResponseDto getActiveCycleByGroupId(UUID groupId) {
        ReadingGroupEntity group = findGroup(groupId);
        validateGroupIsActive(group);

        GroupReadingCycleEntity cycle = cycleRepository
                .findByGroup_IdExternalAndStatus(
                        groupId,
                        GroupReadingCycleStatus.ACTIVE
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active reading cycle not found for group: " + groupId
                ));

        return cycleMapper.toResponseDto(cycle);
    }

    @Override
    @Transactional
    public GroupReadingCycleResponseDto updateBookReference(
            UUID groupId,
            UUID cycleId,
            UpdateGroupReadingCycleRequestDto dto
    ) {
        GroupReadingCycleEntity cycle = findCycle(groupId, cycleId);

        validateGroupIsActive(cycle.getGroup());

        UserEntity authenticatedUser = getAuthenticatedUser();
        validateCanManageCycle(cycle.getGroup(), authenticatedUser);

        if (cycle.getBook() == null) {
            throw new BadRequestException(
                    "Edition and total chapters can only be updated for book reading cycles"
            );
        }

        if (cycle.getStatus() == GroupReadingCycleStatus.COMPLETED
                || cycle.getStatus() == GroupReadingCycleStatus.CANCELLED) {
            throw new BadRequestException(
                    "Completed or cancelled reading cycles cannot be edited"
            );
        }

        cycle.setEditionReference(dto.editionReference().trim());
        cycle.setTotalChapters(dto.totalChapters());

        GroupReadingCycleEntity saved = cycleRepository.save(cycle);

        return cycleMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public GroupReadingCycleResponseDto startCycle(
            UUID groupId,
            UUID cycleId
    ) {
        GroupReadingCycleEntity cycle = findCycle(groupId, cycleId);
        ReadingGroupEntity group = cycle.getGroup();

        validateGroupIsActive(group);

        UserEntity authenticatedUser = getAuthenticatedUser();
        validateCanManageCycle(group, authenticatedUser);

        if (cycle.getStatus() != GroupReadingCycleStatus.PLANNED) {
            throw new BadRequestException(
                    "Only planned reading cycles can be started"
            );
        }

        boolean alreadyHasActiveCycle =
                cycleRepository.existsByGroup_IdExternalAndStatus(
                        group.getIdExternal(),
                        GroupReadingCycleStatus.ACTIVE
                );

        if (alreadyHasActiveCycle) {
            throw new BadRequestException(
                    "This reading group already has an active reading cycle"
            );
        }

        cycle.setStatus(GroupReadingCycleStatus.ACTIVE);
        cycle.setStartedAt(LocalDateTime.now());

        GroupReadingCycleEntity saved = cycleRepository.save(cycle);

        return cycleMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public GroupReadingCycleResponseDto completeCycle(
            UUID groupId,
            UUID cycleId
    ) {
        GroupReadingCycleEntity cycle = findCycle(groupId, cycleId);

        validateGroupIsActive(cycle.getGroup());

        UserEntity authenticatedUser = getAuthenticatedUser();
        validateCanManageCycle(cycle.getGroup(), authenticatedUser);

        if (cycle.getStatus() != GroupReadingCycleStatus.ACTIVE) {
            throw new BadRequestException(
                    "Only active reading cycles can be completed"
            );
        }

        cycle.setStatus(GroupReadingCycleStatus.COMPLETED);
        cycle.setCompletedAt(LocalDateTime.now());

        GroupReadingCycleEntity saved = cycleRepository.save(cycle);

        return cycleMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public GroupReadingCycleResponseDto cancelCycle(
            UUID groupId,
            UUID cycleId
    ) {
        GroupReadingCycleEntity cycle = findCycle(groupId, cycleId);

        validateGroupIsActive(cycle.getGroup());

        UserEntity authenticatedUser = getAuthenticatedUser();
        validateCanManageCycle(cycle.getGroup(), authenticatedUser);

        if (cycle.getStatus() != GroupReadingCycleStatus.PLANNED
                && cycle.getStatus() != GroupReadingCycleStatus.ACTIVE) {
            throw new BadRequestException(
                    "Only planned or active reading cycles can be cancelled"
            );
        }

        cycle.setStatus(GroupReadingCycleStatus.CANCELLED);

        GroupReadingCycleEntity saved = cycleRepository.save(cycle);

        return cycleMapper.toResponseDto(saved);
    }

    private void validateContent(UUID bookId, UUID storyId) {
        if (bookId == null && storyId == null) {
            throw new BadRequestException(
                    "A reading cycle must be associated with either a book or a story"
            );
        }

        if (bookId != null && storyId != null) {
            throw new BadRequestException(
                    "A reading cycle cannot be associated with both a book and a story"
            );
        }
    }

    private void validateCycleReference(GroupReadingCycleRequestDto dto) {

        if (dto.storyId() != null) {
            if (dto.editionReference() != null || dto.totalChapters() != null) {
                throw new BadRequestException(
                        "Edition and total chapters are only allowed for book reading cycles"
                );
            }

            return;
        }

        if (dto.bookId() != null) {
            if (dto.editionReference() == null
                    || dto.editionReference().isBlank()) {
                throw new BadRequestException(
                        "Edition reference is required for book reading cycles"
                );
            }

            if (dto.totalChapters() == null
                    || dto.totalChapters() <= 0) {
                throw new BadRequestException(
                        "Total chapters must be greater than 0 for book reading cycles"
                );
            }
        }
    }

    private void validateCanManageCycle(
            ReadingGroupEntity group,
            UserEntity authenticatedUser
    ) {
        if (isAdminOrModerator(authenticatedUser)) {
            return;
        }

        boolean isCreatorMember = groupMemberRepository
                .existsByGroupIdExternalAndUserIdExternalAndMemberTypeAndStatus(
                        group.getIdExternal(),
                        authenticatedUser.getIdExternal(),
                        GroupMemberType.CREATOR,
                        GroupMemberStatus.ACTIVE
                );

        boolean isOwner = group.getCreatedBy() != null
                && Objects.equals(
                group.getCreatedBy().getId(),
                authenticatedUser.getId()
        );

        if (!isCreatorMember && !isOwner) {
            throw new AccessDeniedException(
                    "Only the group creator, an admin, or a moderator can manage reading cycles"
            );
        }
    }

    private boolean isAdminOrModerator(UserEntity user) {
        return user.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN")
                                || authority.getAuthority().equals("ROLE_MODERATOR")
                );
    }

    private void validateGroupIsActive(ReadingGroupEntity group) {
        if (!Boolean.TRUE.equals(group.getIsActive())) {
            throw new BadRequestException(
                    "Inactive reading groups cannot manage reading cycles"
            );
        }
    }

    private GroupReadingCycleEntity findCycle(
            UUID groupId,
            UUID cycleId
    ) {
        return cycleRepository
                .findByIdExternalAndGroup_IdExternal(cycleId, groupId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reading cycle not found for group: " + groupId
                ));
    }

    private ReadingGroupEntity findGroup(UUID groupId) {
        return readingGroupRepository.findByIdExternal(groupId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reading group not found with idExternal: " + groupId
                ));
    }

    private BookEntity findBook(UUID bookId) {
        return bookRepository.findByIdExternal(bookId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Book not found with idExternal: " + bookId
                ));
    }

    private StoryEntity findStory(UUID storyId) {
        return storyRepository.findByIdExternal(storyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Story not found with idExternal: " + storyId
                ));
    }

    private UserEntity getAuthenticatedUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal() instanceof UserEntity user)) {
            throw new AccessDeniedException(
                    "Authenticated user not found"
            );
        }

        return user;
    }
}