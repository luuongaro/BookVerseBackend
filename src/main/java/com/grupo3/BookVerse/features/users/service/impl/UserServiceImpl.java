package com.grupo3.BookVerse.features.users.service.impl;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.DuplicateResourceException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.roles.domain.RoleEntity;
import com.grupo3.BookVerse.features.roles.repository.RoleRepository;
import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionEntity;
import com.grupo3.BookVerse.features.subscriptions.repository.SubscriptionRepository;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.users.domain.UserStatus;
import com.grupo3.BookVerse.features.users.dto.UserRequestDto;
import com.grupo3.BookVerse.features.users.dto.UserResponseDto;
import com.grupo3.BookVerse.features.users.dto.UserRolesUpdateRequestDto;
import com.grupo3.BookVerse.features.users.dto.UserUpdateRequestDto;
import com.grupo3.BookVerse.features.users.mappers.UserMapper;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import com.grupo3.BookVerse.features.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SubscriptionRepository subscriptionRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponseDto createUser(UserRequestDto dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new DuplicateResourceException("Email is already in use");
        }

        if (userRepository.existsByUsername(dto.username())) {
            throw new DuplicateResourceException("Username is already in use");
        }

        SubscriptionEntity subscription =
                findSubscriptionByIdExternal(dto.subscriptionId());

        RoleEntity defaultRole = roleRepository.findByNameIgnoreCase(DEFAULT_ROLE)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Default role not found: " + DEFAULT_ROLE
                        )
                );

        UserEntity user = userMapper.toEntity(dto);
        user.setPasswordHash(passwordEncoder.encode(dto.password()));
        user.setSubscription(subscription);
        user.getRoles().add(defaultRole);

        UserEntity saved = userRepository.save(user);
        return userMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {
        List<UserEntity> users = userRepository.findAll();
        return userMapper.toResponseDtoList(users);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserByIdExternal(UUID idExternal) {
        UserEntity targetUser = findUserByIdExternal(idExternal);
        UserEntity authenticatedUser = getAuthenticatedUser();

        validateCanViewUser(targetUser, authenticatedUser);

        return userMapper.toResponseDto(targetUser);
    }

    @Override
    @Transactional
    public UserResponseDto updateUser(UUID idExternal, UserUpdateRequestDto dto) {
        UserEntity existing = findUserByIdExternal(idExternal);
        UserEntity authenticatedUser = getAuthenticatedUser();

        validateCanUpdateUser(existing, authenticatedUser);

        if ((existing.getEmail() == null
                || !existing.getEmail().equalsIgnoreCase(dto.email()))
                && userRepository.existsByEmail(dto.email())) {
            throw new DuplicateResourceException("Email is already in use");
        }

        if ((existing.getProfileUsername() == null
                || !existing.getProfileUsername().equalsIgnoreCase(dto.username()))
                && userRepository.existsByUsername(dto.username())) {
            throw new DuplicateResourceException("Username is already in use");
        }

        userMapper.updateEntityFromDto(dto, existing);

        UserEntity updated = userRepository.save(existing);
        return userMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public UserResponseDto updateUserRoles(
            UUID idExternal,
            UserRolesUpdateRequestDto dto
    ) {
        UserEntity targetUser = findUserByIdExternal(idExternal);
        UserEntity authenticatedUser = getAuthenticatedUser();

        if (!hasRole(authenticatedUser, "ROLE_ADMIN")) {
            throw new AccessDeniedException(
                    "Only administrators can update user roles"
            );
        }

        if (dto == null || dto.roleIds() == null || dto.roleIds().isEmpty()) {
            throw new BadRequestException("A user must have at least one role");
        }

        Set<RoleEntity> updatedRoles = new HashSet<>();

        for (UUID roleId : dto.roleIds()) {
            RoleEntity role = roleRepository.findByIdExternal(roleId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Role not found with idExternal: " + roleId
                            )
                    );

            updatedRoles.add(role);
        }

        boolean targetIsAdmin = hasRole(targetUser, "ROLE_ADMIN");
        boolean adminRoleRetained = updatedRoles.stream()
                .anyMatch(role ->
                        "ROLE_ADMIN".equalsIgnoreCase(role.getName())
                );

        if (targetIsAdmin
                && !adminRoleRetained
                && Objects.equals(targetUser.getId(), authenticatedUser.getId())) {
            throw new BadRequestException(
                    "You cannot remove your own administrator role"
            );
        }

        targetUser.setRoles(updatedRoles);

        UserEntity updated = userRepository.save(targetUser);
        return userMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public void deleteUser(UUID idExternal) {
        UserEntity targetUser = findUserByIdExternal(idExternal);
        UserEntity authenticatedUser = getAuthenticatedUser();

        validateCanDeleteUser(authenticatedUser);

        targetUser.setStatus(UserStatus.INACTIVE);
        userRepository.save(targetUser);
    }

    private UserEntity findUserByIdExternal(UUID idExternal) {
        return userRepository.findByIdExternal(idExternal)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with idExternal: " + idExternal
                        )
                );
    }

    private SubscriptionEntity findSubscriptionByIdExternal(UUID idExternal) {
        return subscriptionRepository.findByIdExternal(idExternal)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Subscription not found with idExternal: " + idExternal
                        )
                );
    }

    private UserEntity getAuthenticatedUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal() instanceof UserEntity user)) {
            throw new AccessDeniedException("Authenticated user not found");
        }

        return user;
    }

    private void validateCanViewUser(
            UserEntity targetUser,
            UserEntity authenticatedUser
    ) {
        boolean isSelf = Objects.equals(
                targetUser.getId(),
                authenticatedUser.getId()
        );
        boolean isAdmin = hasRole(authenticatedUser, "ROLE_ADMIN");
        boolean isModerator = hasRole(authenticatedUser, "ROLE_MODERATOR");

        if (!isSelf && !isAdmin && !isModerator) {
            throw new AccessDeniedException(
                    "You do not have permission to view this user"
            );
        }
    }

    private void validateCanUpdateUser(
            UserEntity targetUser,
            UserEntity authenticatedUser
    ) {
        boolean isSelf = Objects.equals(
                targetUser.getId(),
                authenticatedUser.getId()
        );
        boolean isAdmin = hasRole(authenticatedUser, "ROLE_ADMIN");

        if (!isSelf && !isAdmin) {
            throw new AccessDeniedException(
                    "You do not have permission to update this user"
            );
        }
    }

    private void validateCanDeleteUser(UserEntity authenticatedUser) {
        if (!hasRole(authenticatedUser, "ROLE_ADMIN")) {
            throw new AccessDeniedException(
                    "You do not have permission to deactivate this user"
            );
        }
    }

    private boolean hasRole(UserEntity user, String roleName) {
        return user.getAuthorities().stream()
                .anyMatch(authority ->
                        roleName.equals(authority.getAuthority())
                );
    }
}