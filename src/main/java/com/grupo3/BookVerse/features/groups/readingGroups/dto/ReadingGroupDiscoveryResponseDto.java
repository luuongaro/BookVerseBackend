package com.grupo3.BookVerse.features.groups.readingGroups.dto;

import com.grupo3.BookVerse.features.groups.groupReadingCycle.dto.GroupReadingCycleResponseDto;
import com.grupo3.BookVerse.features.groups.readingGroups.domain.GroupMembershipState;

import java.util.UUID;

public record ReadingGroupDiscoveryResponseDto(
        UUID idExternal,
        String name,
        Integer activeMembersCount,
        GroupReadingCycleResponseDto activeReadingCycle,
        String activeContentTitle,
        String activeContentThumbnailUrl,
        GroupMembershipState membershipState
) {
}