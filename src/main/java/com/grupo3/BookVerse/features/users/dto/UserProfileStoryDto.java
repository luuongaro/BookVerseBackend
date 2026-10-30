package com.grupo3.BookVerse.features.users.dto;

import com.grupo3.BookVerse.features.stories.domain.StoryAccessType;
import java.util.UUID;

public record UserProfileStoryDto(UUID idExternal, String title, String description,
                                  StoryAccessType accessType) {}
