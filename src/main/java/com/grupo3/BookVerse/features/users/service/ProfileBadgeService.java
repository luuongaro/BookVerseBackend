package com.grupo3.BookVerse.features.users.service;

import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.users.domain.*;
import com.grupo3.BookVerse.features.users.repository.ProfileBadgeRepository;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileBadgeService {
    private final UserRepository users;
    private final ProfileBadgeRepository badges;

    @Transactional(readOnly = true)
    public ProfileBadgeType get(UUID id) {
        UserEntity user = activeUser(id);
        return badges.findById(user.getId()).map(ProfileBadgeEntity::getBadge).orElse(null);
    }

    @Transactional
    public ProfileBadgeType update(UUID id, ProfileBadgeType badge) {
        UserEntity user = activeUser(id);
        if (badge == null) {
            badges.findById(user.getId()).ifPresent(badges::delete);
            return null;
        }
        ProfileBadgeEntity entity = badges.findById(user.getId()).orElseGet(() -> {
            ProfileBadgeEntity created = new ProfileBadgeEntity();
            created.setUser(user);
            return created;
        });
        entity.setBadge(badge);
        badges.save(entity);
        return badge;
    }

    private UserEntity activeUser(UUID id) {
        return users.findByIdExternalAndStatus(id, UserStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found"));
    }
}
