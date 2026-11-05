package com.grupo3.BookVerse.features.groups.readingGroups.service.impl;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.books.domain.BookEntity;
import com.grupo3.BookVerse.features.books.repository.BookRepository;
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
import com.grupo3.BookVerse.features.groups.readingGroups.service.ReadingGroupService;
import com.grupo3.BookVerse.features.stories.domain.StoryEntity;
import com.grupo3.BookVerse.features.stories.repository.StoryRepository;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReadingGroupServiceImpl implements ReadingGroupService {

    private final ReadingGroupRepository repository;
    private final ReadingGroupMapper mapper;

    private final BookRepository bookRepository;
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Override
    @Transactional
    public ReadingGroupResponseDto createGroup(ReadingGroupRequestDto dto) {
        validateContent(dto.bookId(), dto.storyId());

        UserEntity authenticatedUser = getAuthenticatedUser();
        String normalizedName = normalizeName(dto.name());

        BookEntity book = null;
        StoryEntity story = null;

        if (dto.bookId() != null) {
            book = findBookByIdExternal(dto.bookId());
        }

        if (dto.storyId() != null) {
            story = findStoryByIdExternal(dto.storyId());
        }

        validateUniqueActiveGroupName(normalizedName, dto.bookId(), dto.storyId());

        ReadingGroupEntity entity = mapper.toEntity(dto);
        entity.setName(normalizedName);
        entity.setCreatedBy(authenticatedUser);
        entity.setIsActive(Boolean.TRUE);
        entity.setBook(book);
        entity.setStory(story);

        ReadingGroupEntity saved = repository.save(entity);

        GroupMemberEntity creatorMembership = GroupMemberEntity.builder()
                .group(saved)
                .user(authenticatedUser)
                .memberType(GroupMemberType.CREATOR)
                .status(GroupMemberStatus.ACTIVE)
                .build();

        groupMemberRepository.save(creatorMembership);

        return mapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadingGroupResponseDto> getAllGroups() {
        List<ReadingGroupEntity> activeGroups = repository.findAll().stream()
                .filter(group -> Boolean.TRUE.equals(group.getIsActive()))
                .toList();

        return mapper.toResponseDtoList(activeGroups);
    }

    @Override
    @Transactional(readOnly = true)
    public ReadingGroupResponseDto getGroupByIdExternal(UUID idExternal) {
        ReadingGroupEntity entity = findGroupByIdExternal(idExternal);

        if (!Boolean.TRUE.equals(entity.getIsActive())) {
            throw new ResourceNotFoundException(
                    "Reading group not found with idExternal: " + idExternal
            );
        }

        return mapper.toResponseDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadingGroupResponseDto> getGroupsByBookIdExternal(UUID bookId) {
        findBookByIdExternal(bookId);

        List<ReadingGroupEntity> activeGroups = repository.findByBook_IdExternal(bookId).stream()
                .filter(group -> Boolean.TRUE.equals(group.getIsActive()))
                .toList();

        return mapper.toResponseDtoList(activeGroups);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadingGroupResponseDto> getGroupsByStoryIdExternal(UUID storyId) {
        findStoryByIdExternal(storyId);

        List<ReadingGroupEntity> activeGroups = repository.findByStory_IdExternal(storyId).stream()
                .filter(group -> Boolean.TRUE.equals(group.getIsActive()))
                .toList();

        return mapper.toResponseDtoList(activeGroups);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadingGroupResponseDto> getGroupsByUserIdExternal(UUID userId) {
        findUserByIdExternal(userId);

        List<ReadingGroupEntity> activeGroups = repository.findByCreatedBy_IdExternal(userId).stream()
                .filter(group -> Boolean.TRUE.equals(group.getIsActive()))
                .toList();

        return mapper.toResponseDtoList(activeGroups);
    }

    @Override
    @Transactional
    public ReadingGroupResponseDto updateGroup(UUID idExternal, UpdateReadingGroupRequestDto dto) {
        ReadingGroupEntity entity = findGroupByIdExternal(idExternal);
        UserEntity authenticatedUser = getAuthenticatedUser();
        String normalizedName = normalizeName(dto.name());

        validateCanManageGroup(entity, authenticatedUser);

        if (!Boolean.TRUE.equals(entity.getIsActive())) {
            throw new BadRequestException("Inactive reading groups cannot be updated");
        }

        validateUniqueActiveGroupNameForUpdate(entity, normalizedName);

        if (dto.totalChapters() != null) {
            if (entity.getBook() == null) {
                throw new BadRequestException(
                        "Total chapters can only be configured for book groups"
                );
            }

            if (dto.totalChapters() <= 0) {
                throw new BadRequestException(
                        "Total chapters must be greater than 0"
                );
            }

            entity.setTotalChapters(dto.totalChapters());
        }

        entity.setName(normalizedName);

        ReadingGroupEntity updated = repository.save(entity);
        return mapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public void deleteGroup(UUID idExternal) {
        ReadingGroupEntity entity = findGroupByIdExternal(idExternal);
        UserEntity authenticatedUser = getAuthenticatedUser();

        validateCanManageGroup(entity, authenticatedUser);

        if (!Boolean.TRUE.equals(entity.getIsActive())) {
            throw new BadRequestException("Reading group is already inactive");
        }

        entity.setIsActive(Boolean.FALSE);
        repository.save(entity);
    }

    private void validateContent(UUID bookId, UUID storyId) {
        if (bookId == null && storyId == null) {
            throw new BadRequestException(
                    "A reading group must be associated with either a book or a story"
            );
        }

        if (bookId != null && storyId != null) {
            throw new BadRequestException(
                    "A reading group cannot be associated with both a book and a story"
            );
        }
    }

    private void validateUniqueActiveGroupName(String name, UUID bookId, UUID storyId) {
        if (bookId != null) {
            boolean exists = repository.existsByNameIgnoreCaseAndBook_IdExternalAndIsActiveTrue(name, bookId);
            if (exists) {
                throw new BadRequestException(
                        "An active reading group with this name already exists for this book"
                );
            }
        }

        if (storyId != null) {
            boolean exists = repository.existsByNameIgnoreCaseAndStory_IdExternalAndIsActiveTrue(name, storyId);
            if (exists) {
                throw new BadRequestException(
                        "An active reading group with this name already exists for this story"
                );
            }
        }
    }

    private void validateUniqueActiveGroupNameForUpdate(ReadingGroupEntity entity, String name) {
        if (entity.getBook() != null) {
            boolean exists = repository.existsByNameIgnoreCaseAndBook_IdExternalAndIsActiveTrueAndIdExternalNot(
                    name,
                    entity.getBook().getIdExternal(),
                    entity.getIdExternal()
            );

            if (exists) {
                throw new BadRequestException(
                        "An active reading group with this name already exists for this book"
                );
            }
        }

        if (entity.getStory() != null) {
            boolean exists = repository.existsByNameIgnoreCaseAndStory_IdExternalAndIsActiveTrueAndIdExternalNot(
                    name,
                    entity.getStory().getIdExternal(),
                    entity.getIdExternal()
            );

            if (exists) {
                throw new BadRequestException(
                        "An active reading group with this name already exists for this story"
                );
            }
        }
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private void validateCanManageGroup(ReadingGroupEntity entity, UserEntity authenticatedUser) {
        boolean isOwner = entity.getCreatedBy() != null
                && Objects.equals(entity.getCreatedBy().getId(), authenticatedUser.getId());

        if (!isOwner && !isAdminOrModerator(authenticatedUser)) {
            throw new AccessDeniedException(
                    "You do not have permission to manage this reading group"
            );
        }
    }

    private boolean isAdminOrModerator(UserEntity user) {
        return user.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN")
                                || authority.getAuthority().equals("ROLE_MODERATOR"));
    }

    private ReadingGroupEntity findGroupByIdExternal(UUID idExternal) {
        return repository.findByIdExternal(idExternal)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Reading group not found with idExternal: " + idExternal
                        ));
    }

    private BookEntity findBookByIdExternal(UUID idExternal) {
        return bookRepository.findByIdExternal(idExternal)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Book not found with idExternal: " + idExternal
                        ));
    }

    private StoryEntity findStoryByIdExternal(UUID idExternal) {
        return storyRepository.findByIdExternal(idExternal)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Story not found with idExternal: " + idExternal
                        ));
    }

    private UserEntity findUserByIdExternal(UUID idExternal) {
        return userRepository.findByIdExternal(idExternal)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with idExternal: " + idExternal
                        ));
    }

    private UserEntity getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserEntity user)) {
            throw new AccessDeniedException("Authenticated user not found");
        }

        return user;
    }
}