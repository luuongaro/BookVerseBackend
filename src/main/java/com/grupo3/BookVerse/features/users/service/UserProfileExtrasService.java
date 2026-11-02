package com.grupo3.BookVerse.features.users.service;

import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.groups.groupMember.repository.GroupMemberRepository;
import com.grupo3.BookVerse.features.status.domain.ReadingStatusEnum;
import com.grupo3.BookVerse.features.status.repository.ReadingStatusRepository;
import com.grupo3.BookVerse.features.stories.repository.StoryRepository;
import com.grupo3.BookVerse.features.users.domain.*;
import com.grupo3.BookVerse.features.users.dto.*;
import com.grupo3.BookVerse.features.users.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileExtrasService {
    private final UserRepository users;
    private final StoryRepository stories;
    private final ReadingStatusRepository readingStatuses;
    private final GroupMemberRepository members;
    private final ProfileAppearanceRepository appearances;
    private final FavoriteStoryRepository favorites;

    public ProfileActivityDto activity(UUID userId) {
        var user = activeUser(userId);
        return new ProfileActivityDto(
                readingStatuses.countByUserIdAndStatusIn(user.getId(), List.of(ReadingStatusEnum.WANT_TO_READ)),
                readingStatuses.countByUserIdAndStatusIn(user.getId(), List.of(ReadingStatusEnum.IN_PROGRESS, ReadingStatusEnum.RE_READING)),
                readingStatuses.countByUserIdAndStatusIn(user.getId(), List.of(ReadingStatusEnum.FINISHED)),
                stories.countByAuthorIdAndDeletedFalse(user.getId()),
                members.countActiveGroupsByUser(user.getId()),
                stories.findPublishedForAuthor(userId, org.springframework.data.domain.PageRequest.of(0, 1))
                        .stream().findFirst().map(story -> story.getIdExternal()).orElse(null)
        );
    }

    public ProfileAppearanceDto appearance(UUID userId) {
        var user = activeUser(userId);
        return appearances.findById(user.getId()).map(value -> new ProfileAppearanceDto(value.getCoverIndex(), value.getAvatarIndex(), true))
                .orElse(new ProfileAppearanceDto(0, 0, false));
    }

    @Transactional
    public ProfileAppearanceDto updateAppearance(ProfileAppearanceRequestDto request) {
        var user = currentUser();
        var appearance = appearances.findById(user.getId()).orElseGet(() -> {
            var value = new ProfileAppearanceEntity();
            value.setUser(user);
            return value;
        });
        if (request.coverIndex() != null) appearance.setCoverIndex(request.coverIndex());
        if (request.avatarIndex() != null) appearance.setAvatarIndex(request.avatarIndex());
        appearances.save(appearance);
        return new ProfileAppearanceDto(appearance.getCoverIndex(), appearance.getAvatarIndex(), true);
    }

    public Page<UserProfileStoryDto> favorites(UUID userId, Pageable pageable) {
        activeUser(userId);
        return favorites.findVisibleFavorites(userId, pageable).map(favorite -> {
            var story = favorite.getStory();
            return new UserProfileStoryDto(story.getIdExternal(), story.getTitle(), story.getDescription(), story.getAccessType());
        });
    }

    public boolean isFavorite(UUID storyId) {
        var user = currentUser();
        return favorites.findByUserIdExternalAndStoryIdExternal(user.getIdExternal(), storyId).isPresent();
    }

    @Transactional
    public void addFavorite(UUID storyId) {
        var user = currentUser();
        var story = stories.findByIdExternalAndDeletedFalse(storyId)
                .filter(value -> !value.isHidden())
                .orElseThrow(() -> new ResourceNotFoundException("Story not available"));
        if (favorites.findByUserIdExternalAndStoryIdExternal(user.getIdExternal(), storyId).isPresent()) return;
        var favorite = new FavoriteStoryEntity();
        favorite.setUser(user);
        favorite.setStory(story);
        favorites.save(favorite);
    }

    @Transactional
    public void removeFavorite(UUID storyId) {
        var user = currentUser();
        favorites.findByUserIdExternalAndStoryIdExternal(user.getIdExternal(), storyId).ifPresent(favorites::delete);
    }

    private UserEntity activeUser(UUID id) {
        return users.findByIdExternalAndStatus(id, UserStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found"));
    }

    private UserEntity currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserEntity user)
                || user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("An active authenticated user is required");
        }
        return user;
    }
}
