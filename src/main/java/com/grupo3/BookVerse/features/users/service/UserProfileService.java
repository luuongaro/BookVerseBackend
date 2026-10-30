package com.grupo3.BookVerse.features.users.service;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.stories.repository.StoryRepository;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.users.domain.UserStatus;
import com.grupo3.BookVerse.features.users.dto.UserProfileResponseDto;
import com.grupo3.BookVerse.features.users.dto.UserProfileStoryDto;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileService {
    private final UserRepository userRepository;
    private final StoryRepository storyRepository;

    public Page<UserProfileResponseDto> search(String username, Pageable pageable) {
        String query = username.trim().replaceFirst("^@", "");
        if (query.length() > 100) {
            throw new BadRequestException("Username search cannot exceed 100 characters");
        }
        return userRepository.searchProfiles(query, UserStatus.ACTIVE, pageable);
    }

    public UserProfileResponseDto getProfile(UUID id) {
        UserEntity user = findActiveUser(id);
        return new UserProfileResponseDto(user.getIdExternal(), user.getProfileUsername());
    }

    public Page<UserProfileStoryDto> getStories(UUID id, Pageable pageable) {
        UserEntity user = findActiveUser(id);
        return storyRepository.findByAuthorIdAndDeletedFalseAndHiddenFalseOrderByCreatedAtDescIdDesc(
                user.getId(), pageable
        ).map(story -> new UserProfileStoryDto(story.getIdExternal(), story.getTitle(),
                story.getDescription(), story.getAccessType()));
    }

    private UserEntity findActiveUser(UUID id) {
        return userRepository.findByIdExternalAndStatus(id, UserStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found"));
    }
}
