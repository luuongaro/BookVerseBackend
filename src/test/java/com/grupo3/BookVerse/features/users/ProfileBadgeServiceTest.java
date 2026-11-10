package com.grupo3.BookVerse.features.users;

import com.grupo3.BookVerse.features.users.domain.*;
import com.grupo3.BookVerse.features.users.repository.ProfileBadgeRepository;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import com.grupo3.BookVerse.features.users.service.ProfileBadgeService;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfileBadgeServiceTest {
    @Test void assignsReplacesAndRemovesTheOnlyBadgeOfAUser() {
        var users = mock(UserRepository.class);
        var badges = mock(ProfileBadgeRepository.class);
        var service = new ProfileBadgeService(users, badges);
        UUID id = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(42L).idExternal(id).build();
        when(users.findByIdExternalAndStatus(id, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(badges.findById(42L)).thenReturn(Optional.empty());
        assertNull(service.get(id));
        assertEquals(ProfileBadgeType.FOUNDING_WRITER, service.update(id, ProfileBadgeType.FOUNDING_WRITER));
        var captor = org.mockito.ArgumentCaptor.forClass(ProfileBadgeEntity.class);
        verify(badges).save(captor.capture());
        ProfileBadgeEntity assigned = captor.getValue();
        assertSame(user, assigned.getUser());
        when(badges.findById(42L)).thenReturn(Optional.of(assigned));
        service.update(id, ProfileBadgeType.FOUNDING_READER_2);
        assertEquals(ProfileBadgeType.FOUNDING_READER_2, assigned.getBadge());
        service.update(id, null);
        verify(badges).delete(assigned);
    }
}
