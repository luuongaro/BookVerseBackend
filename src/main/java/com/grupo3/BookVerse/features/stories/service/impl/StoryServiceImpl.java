package com.grupo3.BookVerse.features.stories.service.impl;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.chapters.domain.ChapterEntity;
import com.grupo3.BookVerse.features.chapters.mappers.ChapterMapper;
import com.grupo3.BookVerse.features.stories.domain.StoryAccessType;
import com.grupo3.BookVerse.features.stories.domain.StoryEntity;
import com.grupo3.BookVerse.features.stories.dto.StoryDetailResponseDto;
import com.grupo3.BookVerse.features.stories.dto.StoryRequestDto;
import com.grupo3.BookVerse.features.stories.dto.StoryResponseDto;
import com.grupo3.BookVerse.features.stories.mappers.StoryMapper;
import com.grupo3.BookVerse.features.stories.repository.StoryRepository;
import com.grupo3.BookVerse.features.stories.service.StoryService;
import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionType;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoryServiceImpl implements StoryService {

    private final StoryRepository storyRepository;
    private final StoryMapper storyMapper;
    private final ChapterMapper chapterMapper;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public StoryResponseDto createStory(StoryRequestDto storyRequestDto) {

        UserEntity authenticatedUser = getAuthenticatedUser();
        UserEntity author = findUserByIdExternal(storyRequestDto.getAuthorId());

        validateAuthenticatedUserMatchesAuthor(authenticatedUser, author);
        validateAuthorCanPublishStory(author);
        validateAuthorCanPublishPremiumStory(author, storyRequestDto);

        StoryEntity storyEntity = storyMapper.toEntity(storyRequestDto);
        storyEntity.setAuthor(author);

        StoryEntity savedStory = storyRepository.save(storyEntity);

        return storyMapper.toResponseDto(savedStory);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StoryResponseDto> getAllStories(Pageable pageable) {

        Page<StoryEntity> stories =
                storyRepository.findAllByDeletedFalseOrderByCreatedAtDesc(pageable);

        return stories.map(storyMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StoryResponseDto> getStoriesToExplore(Pageable pageable) {
        UserEntity reader = getAuthenticatedUser();
        return storyRepository
                .findByAuthorIdNotAndDeletedFalseAndHiddenFalseOrderByCreatedAtDesc(reader.getId(), pageable)
                .map(storyMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public StoryDetailResponseDto getStoryByIdExternal(UUID idExternal) {

        StoryEntity story = findActiveStoryByIdExternal(idExternal);
        UserEntity authenticatedUser = getAuthenticatedUser();

        validateCanAccessStory(story, authenticatedUser);

        boolean canManage = canManageStory(story, authenticatedUser);

        List<ChapterEntity> visibleChapters =
                story.getChapters() == null
                        ? List.of()
                        : story.getChapters()
                        .stream()
                        .filter(chapter ->
                                !chapter.isDeleted()
                                        && (
                                        canManage
                                                || chapter.isPublished()
                                )
                        )
                        .sorted(
                                Comparator.comparingInt(
                                        ChapterEntity::getChapterNumber
                                )
                        )
                        .toList();

        return StoryDetailResponseDto.builder()
                .idExternal(story.getIdExternal())
                .authorId(story.getAuthor().getIdExternal())
                .title(story.getTitle())
                .description(story.getDescription())
                .accessType(story.getAccessType())
                .hidden(story.isHidden())
                .deleted(story.isDeleted())
                .createdAt(story.getCreatedAt())
                .updatedAt(story.getUpdatedAt())
                .chaptersCount(visibleChapters.size())
                .chapters(chapterMapper.toSummaryDtoList(visibleChapters))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StoryResponseDto> getStoriesByAuthorId(UUID authorId, Pageable pageable) {

        UserEntity author = findUserByIdExternal(authorId);

        Page<StoryEntity> stories =
                storyRepository.findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(author.getId(), pageable);

        return stories.map(storyMapper::toResponseDto);
    }

    @Override
    @Transactional
    public StoryResponseDto updateStory(UUID idExternal, StoryRequestDto storyRequestDto) {

        StoryEntity existingStory = findActiveStoryByIdExternal(idExternal);

        UserEntity authenticatedUser = getAuthenticatedUser();

        validateCanManageStory(existingStory, authenticatedUser);

        validateAuthorCanPublishPremiumStory(existingStory.getAuthor(), storyRequestDto);

        existingStory.setTitle(storyRequestDto.getTitle());

        existingStory.setDescription(storyRequestDto.getDescription());

        existingStory.setAccessType(storyRequestDto.getAccessType());

        StoryEntity updatedStory = storyRepository.save(existingStory);

        return storyMapper.toResponseDto(updatedStory);
    }

    @Override
    @Transactional
    public void deleteStory(UUID idExternal) {

        StoryEntity story = findActiveStoryByIdExternal(idExternal);
        UserEntity authenticatedUser = getAuthenticatedUser();

        validateCanManageStory(story, authenticatedUser);

        story.setDeleted(true);
        storyRepository.save(story);
    }

    private StoryEntity findActiveStoryByIdExternal(UUID idExternal) {
        return storyRepository.findByIdExternalAndDeletedFalse(idExternal)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Story not found with idExternal: " + idExternal
                        )
                );
    }

    private UserEntity findUserByIdExternal(UUID idExternal) {
        return userRepository.findByIdExternal(idExternal)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with idExternal: " + idExternal
                        )
                );
    }

    private UserEntity getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserEntity user)) {
            throw new AccessDeniedException("Authenticated user not found");
        }

        return user;
    }

    private void validateAuthenticatedUserMatchesAuthor(UserEntity authenticatedUser, UserEntity author) {
        if (!authenticatedUser.getId().equals(author.getId())) {
            throw new AccessDeniedException("You cannot create a story on behalf of another user");
        }
    }

    private void validateAuthorCanPublishStory(UserEntity author) {
        if (author.getSubscription() == null) {
            throw new AccessDeniedException("User subscription not found");
        }

        int maxStoriesPublished = author.getSubscription().getMaxStoriesPublished();
        long currentPublishedStories = storyRepository.countByAuthorIdAndDeletedFalse(author.getId());

        if (currentPublishedStories >= maxStoriesPublished) {
            throw new BadRequestException(
                    "You have reached the maximum number of stories allowed by your current subscription"
            );
        }
    }

    private void validateCanAccessStory(StoryEntity story, UserEntity user) {
        boolean isOwner = story.getAuthor() != null
                && story.getAuthor().getId().equals(user.getId());

        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        boolean isModerator = user.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_MODERATOR".equals(authority.getAuthority()));

        boolean isPremiumStory = story.getAccessType() == StoryAccessType.PREMIUM;
        boolean isPremiumUser = user.getSubscription() != null
                && user.getSubscription().getType() == SubscriptionType.PREMIUM;

        if (isPremiumStory && !isOwner && !isAdmin && !isModerator && !isPremiumUser) {
            throw new AccessDeniedException("You need a premium subscription to access this story");
        }
    }

    private void validateCanManageStory(StoryEntity story, UserEntity user) {
        boolean isOwner = story.getAuthor() != null
                && story.getAuthor().getId().equals(user.getId());

        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        boolean isModerator = user.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_MODERATOR".equals(authority.getAuthority()));

        if (!isOwner && !isAdmin && !isModerator) {
            throw new AccessDeniedException("You do not have permission to manage this story");
        }
    }

    private void validateAuthorCanPublishPremiumStory(UserEntity author, StoryRequestDto storyRequestDto) {
        boolean isPremiumStory = storyRequestDto.getAccessType() == StoryAccessType.PREMIUM;
        boolean isPremiumUser = author.getSubscription() != null
                && author.getSubscription().getType() == SubscriptionType.PREMIUM;

        if (isPremiumStory && !isPremiumUser) {
            throw new AccessDeniedException("Only premium users can publish premium stories");
        }
    }

    private boolean canManageStory(StoryEntity story, UserEntity user) {

        boolean isOwner =
                story.getAuthor() != null
                        && story.getAuthor()
                        .getId()
                        .equals(user.getId());

        boolean isAdmin =
                user.getAuthorities()
                        .stream()
                        .anyMatch(
                                authority ->
                                        "ROLE_ADMIN".equals(
                                                authority.getAuthority()
                                        )
                        );

        boolean isModerator =
                user.getAuthorities()
                        .stream()
                        .anyMatch(
                                authority ->
                                        "ROLE_MODERATOR".equals(
                                                authority.getAuthority()
                                        )
                        );

        return isOwner || isAdmin || isModerator;
    }
}
