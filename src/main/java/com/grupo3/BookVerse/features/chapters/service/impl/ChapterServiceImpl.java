package com.grupo3.BookVerse.features.chapters.service.impl;

import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.chapters.domain.ChapterEntity;
import com.grupo3.BookVerse.features.chapters.dto.ChapterCreateRequestDto;
import com.grupo3.BookVerse.features.chapters.dto.ChapterResponseDto;
import com.grupo3.BookVerse.features.chapters.dto.ChapterUpdateRequestDto;
import com.grupo3.BookVerse.features.chapters.mappers.ChapterMapper;
import com.grupo3.BookVerse.features.chapters.repository.ChapterRepository;
import com.grupo3.BookVerse.features.chapters.service.ChapterService;
import com.grupo3.BookVerse.features.stories.domain.StoryAccessType;
import com.grupo3.BookVerse.features.stories.domain.StoryEntity;
import com.grupo3.BookVerse.features.stories.repository.StoryRepository;
import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionType;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ChapterServiceImpl implements ChapterService {

    private final ChapterRepository chapterRepository;
    private final ChapterMapper chapterMapper;
    private final StoryRepository storyRepository;

    @Override
    @Transactional
    public ChapterResponseDto createChapter(ChapterCreateRequestDto dto) {

        StoryEntity story = findActiveStoryByIdExternal(dto.getStoryId());
        UserEntity authenticatedUser = getAuthenticatedUser();

        validateCanManageStory(story, authenticatedUser);

        int nextChapterNumber = chapterRepository
                .findTopByStoryIdOrderByChapterNumberDesc(story.getId())
                .map(chapter -> chapter.getChapterNumber() + 1)
                .orElse(1);

        ChapterEntity chapter = chapterMapper.toEntity(dto);
        chapter.setStory(story);
        chapter.setChapterNumber(nextChapterNumber);

        ChapterEntity saved = chapterRepository.save(chapter);
        return chapterMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChapterResponseDto> getAllChapters(Pageable pageable) {
        Page<ChapterEntity> chapters =
                chapterRepository.findAllByDeletedFalseOrderByCreatedAtDesc(pageable);

        return chapters.map(chapterMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ChapterResponseDto getChapterByIdExternal(
            UUID idExternal
    ) {
        ChapterEntity chapter = findActiveChapterByIdExternal(idExternal);

        validateParentStoryIsActive(chapter);

        UserEntity authenticatedUser = getAuthenticatedUser();

        validateCanReadStory(chapter.getStory(), authenticatedUser);

        validateCanReadChapter(chapter, authenticatedUser);

        return chapterMapper.toResponseDto(
                chapter
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChapterResponseDto> getChaptersByStoryId(UUID storyId, Pageable pageable)
    {
        return getChaptersByStoryId(storyId, pageable, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChapterResponseDto> getChaptersByStoryId(UUID storyId, Pageable pageable, boolean publishedOnly)
    {
        StoryEntity story = findActiveStoryByIdExternal(storyId);

        UserEntity authenticatedUser = getAuthenticatedUser();

        validateCanReadStory(story, authenticatedUser);

        Page<ChapterEntity> chapters;

        if (!publishedOnly && canManageStory(story, authenticatedUser)) {
            chapters = chapterRepository.findByStoryIdAndDeletedFalseOrderByChapterNumberAsc(story.getId(), pageable);
        } else {
            chapters = chapterRepository.findByStoryIdAndDeletedFalseAndPublishedTrueOrderByChapterNumberAsc(story.getId(), pageable);
        }

        return chapters.map(chapterMapper::toResponseDto);
    }

    @Override
    @Transactional
    public ChapterResponseDto updateChapter(UUID idExternal, ChapterUpdateRequestDto dto) {
        ChapterEntity existing = findActiveChapterByIdExternal(idExternal);
        validateParentStoryIsActive(existing);

        UserEntity authenticatedUser = getAuthenticatedUser();
        validateCanManageStory(existing.getStory(), authenticatedUser);

        existing.setTitle(dto.getTitle());
        existing.setContent(dto.getContent());
        existing.setPublished(dto.isPublished());

        ChapterEntity saved = chapterRepository.save(existing);
        return chapterMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public void deleteChapter(UUID idExternal) {
        ChapterEntity existing = findActiveChapterByIdExternal(idExternal);
        validateParentStoryIsActive(existing);

        UserEntity authenticatedUser = getAuthenticatedUser();
        validateCanManageStory(existing.getStory(), authenticatedUser);

        existing.setDeleted(true);
        chapterRepository.save(existing);
    }

    private ChapterEntity findActiveChapterByIdExternal(UUID idExternal) {
        return chapterRepository.findByIdExternalAndDeletedFalse(idExternal)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found"));
    }

    private StoryEntity findActiveStoryByIdExternal(UUID storyId) {
        return storyRepository.findByIdExternalAndDeletedFalse(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found"));
    }

    private void validateParentStoryIsActive(ChapterEntity chapter) {
        StoryEntity story = chapter.getStory();

        if (story == null || story.isDeleted()) {
            throw new ResourceNotFoundException("Parent story not found or deleted");
        }
    }

    private UserEntity getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserEntity user)) {
            throw new AccessDeniedException("Authenticated user not found");
        }

        return user;
    }

    private void validateCanManageStory(StoryEntity story, UserEntity user) {
        boolean isOwner = story.getAuthor() != null
                && story.getAuthor().getId().equals(user.getId());

        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        boolean isModerator = user.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_MODERATOR".equals(authority.getAuthority()));

        if (!isOwner && !isAdmin && !isModerator) {
            throw new AccessDeniedException("You do not have permission to manage chapters for this story");
        }
    }

    private void validateCanReadStory(StoryEntity story, UserEntity user)
    {
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

        boolean isPremiumStory =
                story.getAccessType()
                        == StoryAccessType.PREMIUM;

        boolean isPremiumUser =
                user.getSubscription() != null
                        && user.getSubscription().getType()
                        == SubscriptionType.PREMIUM;

        if (
                isPremiumStory
                        && !isOwner
                        && !isAdmin
                        && !isModerator
                        && !isPremiumUser
        ) {
            throw new AccessDeniedException(
                    "You need a PREMIUM subscription to read this story"
            );
        }
    }

    private void validateCanReadChapter(
            ChapterEntity chapter,
            UserEntity user
    ) {
        if (chapter.isPublished()) {
            return;
        }

        StoryEntity story =
                chapter.getStory();

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

        if (!isOwner && !isAdmin && !isModerator) {
            throw new AccessDeniedException(
                    "This chapter has not been published"
            );
        }
    }


    private boolean canManageStory(
            StoryEntity story,
            UserEntity user
    ) {
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
