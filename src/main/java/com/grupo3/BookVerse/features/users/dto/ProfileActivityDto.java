package com.grupo3.BookVerse.features.users.dto;

public record ProfileActivityDto(long wantToRead, long inProgress, long finished,
                                 long createdStories, long activeGroups, java.util.UUID tipStoryId) {}
