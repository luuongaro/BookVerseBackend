package com.grupo3.BookVerse.features.users.service.impl;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.DuplicateResourceException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberEntity;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberStatus;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberType;
import com.grupo3.BookVerse.features.groups.groupMember.repository.GroupMemberRepository;
import com.grupo3.BookVerse.features.groups.readingGroups.domain.ReadingGroupEntity;
import com.grupo3.BookVerse.features.groups.readingGroups.repository.ReadingGroupRepository;
import com.grupo3.BookVerse.features.roles.domain.RoleEntity;
import com.grupo3.BookVerse.features.roles.repository.RoleRepository;
import com.grupo3.BookVerse.features.stories.domain.StoryEntity;
import com.grupo3.BookVerse.features.stories.repository.StoryRepository;
import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionEntity;
import com.grupo3.BookVerse.features.subscriptions.repository.SubscriptionRepository;
import com.grupo3.BookVerse.features.users.domain.AccountStoryAction;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.users.domain.UserStatus;
import com.grupo3.BookVerse.features.users.dto.*;
import com.grupo3.BookVerse.features.users.mappers.UserMapper;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import com.grupo3.BookVerse.features.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_ROLE = "ROLE_USER";

    @Value("${app.admin.email}")
    private String protectedAdminEmail;

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SubscriptionRepository subscriptionRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final GroupMemberRepository groupMemberRepository;
    private final ReadingGroupRepository readingGroupRepository;
    private final StoryRepository storyRepository;

    @Override
    @Transactional
    public UserResponseDto createUser(UserRequestDto dto) {

        String normalizedEmail =
                normalizeEmail(dto.email());

        String normalizedUsername =
                dto.username().trim();

        Optional<UserEntity> existingUserOptional =
                userRepository.findByEmailWithRoles(
                        normalizedEmail
                );

        if (existingUserOptional.isPresent()) {

            UserEntity existingUser =
                    existingUserOptional.get();

            if (existingUser.getStatus() == UserStatus.ACTIVE) {
                throw new DuplicateResourceException(
                        "Email is already in use"
                );
            }

            if (existingUser.getStatus() == UserStatus.SUSPENDED) {
                throw new BadRequestException(
                        "Suspended accounts cannot be reactivated through registration"
                );
            }

            if (existingUser.getStatus() == UserStatus.BANNED) {
                throw new BadRequestException(
                        "Banned accounts cannot be reactivated through registration"
                );
            }

            if (existingUser.getStatus() != UserStatus.INACTIVE) {
                throw new BadRequestException(
                        "This account cannot be reactivated through registration"
                );
            }

            return reactivateUser(
                    existingUser,
                    normalizedUsername,
                    dto.password()
            );
        }

        if (userRepository.existsByUsernameIgnoreCase(
                normalizedUsername
        )) {
            throw new DuplicateResourceException(
                    "Username is already in use"
            );
        }

        SubscriptionEntity subscription =
                findSubscriptionByIdExternal(
                        dto.subscriptionId()
                );

        RoleEntity defaultRole =
                roleRepository
                        .findByNameIgnoreCase(DEFAULT_ROLE)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Default role not found: "
                                                + DEFAULT_ROLE
                                )
                        );

        UserEntity user =
                userMapper.toEntity(dto);

        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(
                passwordEncoder.encode(dto.password())
        );
        user.setSubscription(subscription);
        user.getRoles().add(defaultRole);

        UserEntity saved =
                userRepository.save(user);

        return userMapper.toResponseDto(saved);
    }

    private UserResponseDto reactivateUser(
            UserEntity existingUser,
            String requestedUsername,
            String newPassword
    ) {
        boolean usernameBelongsToAnotherUser =
                userRepository
                        .existsByUsernameIgnoreCaseAndIdNot(
                                requestedUsername,
                                existingUser.getId()
                        );

        if (usernameBelongsToAnotherUser) {
            throw new DuplicateResourceException(
                    "Username is already in use"
            );
        }

        existingUser.setUsername(requestedUsername);

        existingUser.setPasswordHash(
                passwordEncoder.encode(newPassword)
        );

        existingUser.setCredentialsVersion(
                existingUser.getCredentialsVersion() + 1
        );

        existingUser.setStatus(UserStatus.ACTIVE);

        UserEntity reactivatedUser =
                userRepository.save(existingUser);

        return userMapper.toResponseDto(
                reactivatedUser
        );
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

        if (isProtectedAdmin(existing)
                && !existing.getEmail().equalsIgnoreCase(
                dto.email().trim()
        )) {
            throw new BadRequestException(
                    "The protected administrator email cannot be changed"
            );
        }

        if ((existing.getEmail() == null
                || !existing.getEmail().equalsIgnoreCase(dto.email()))
                && userRepository.existsByEmailIgnoreCase(dto.email().trim())) {
            throw new DuplicateResourceException("Email is already in use");
        }

        if ((existing.getProfileUsername() == null
                || !existing.getProfileUsername().equalsIgnoreCase(dto.username()))
                && userRepository.existsByUsernameIgnoreCase(dto.username().trim())) {
            throw new DuplicateResourceException("Username is already in use");
        }

        userMapper.updateEntityFromDto(dto, existing);
        existing.setUsername(dto.username().trim());
        existing.setEmail(normalizeEmail(dto.email()));

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

        if (isProtectedAdmin(targetUser)
                && !adminRoleRetained) {
            throw new BadRequestException(
                    "The protected administrator must always retain ROLE_ADMIN"
            );
        }

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

        validateCanDeleteUser(
                targetUser,
                authenticatedUser
        );

        targetUser.setStatus(UserStatus.INACTIVE);
        userRepository.save(targetUser);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
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

    private void validateCanDeleteUser(
            UserEntity targetUser,
            UserEntity authenticatedUser
    ) {
        if (!hasRole(authenticatedUser, "ROLE_ADMIN")) {
            throw new AccessDeniedException(
                    "You do not have permission to deactivate this user"
            );
        }

        if (isProtectedAdmin(targetUser)) {
            throw new BadRequestException(
                    "The protected administrator cannot be deactivated"
            );
        }
    }

    private boolean isProtectedAdmin(UserEntity user) {
        return user != null
                && user.getEmail() != null
                && protectedAdminEmail != null
                && user.getEmail().trim().equalsIgnoreCase(
                protectedAdminEmail.trim()
        );
    }

    private boolean hasRole(UserEntity user, String roleName) {
        return user.getAuthorities().stream()
                .anyMatch(authority ->
                        roleName.equals(authority.getAuthority())
                );
    }

    @Override
    @Transactional
    public void deactivateOwnAccount(AccountDeactivationRequestDto dto) {

        if (dto == null) {
            throw new BadRequestException(
                    "Account deactivation request is required"
            );
        }

        UserEntity authenticatedUser = getAuthenticatedUser();

        if (isProtectedAdmin(authenticatedUser)) {
            throw new BadRequestException(
                    "The protected administrator cannot be deactivated"
            );
        }

        if (authenticatedUser.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException(
                    "Only an active account can be deactivated"
            );
        }

        processGroupMemberships(
                authenticatedUser,
                dto.creatorReplacements()
        );

        processUserStories(
                authenticatedUser,
                dto.storyAction()
        );

        authenticatedUser.setStatus(UserStatus.INACTIVE);
        userRepository.save(authenticatedUser);
    }

    private void processGroupMemberships(
            UserEntity user,
            Map<UUID, String> creatorReplacements
    ) {

        List<GroupMemberEntity> memberships =
                groupMemberRepository.findByUserIdExternalAndStatus(
                        user.getIdExternal(),
                        GroupMemberStatus.ACTIVE
                );

        for (GroupMemberEntity membership : memberships) {

            if (membership.getMemberType() == GroupMemberType.CREATOR) {
                processCreatorMembership(
                        membership,
                        user,
                        creatorReplacements
                );
            } else {
                membership.setStatus(GroupMemberStatus.LEFT);

                groupMemberRepository.save(membership);
            }
        }
    }

    private void processCreatorMembership(
            GroupMemberEntity creatorMembership,
            UserEntity user,
            Map<UUID, String> creatorReplacements
    ) {

        ReadingGroupEntity group = creatorMembership.getGroup();

        List<GroupMemberEntity> otherMembers =
                groupMemberRepository
                        .findByGroupIdExternalAndStatus(
                                group.getIdExternal(),
                                GroupMemberStatus.ACTIVE
                        )
                        .stream()
                        .filter(member ->
                                !Objects.equals(
                                        member.getUser().getId(),
                                        user.getId()
                                )
                        )
                        .filter(member ->
                                member.getUser().getStatus() == UserStatus.ACTIVE
                        )
                        .toList();

        if (otherMembers.isEmpty()) {

            group.setIsActive(false);
            readingGroupRepository.save(group);

            creatorMembership.setStatus(GroupMemberStatus.LEFT);

            groupMemberRepository.save(creatorMembership);

            return;
        }

        GroupMemberEntity newCreator =
                findReplacementCreator(
                        group,
                        otherMembers,
                        creatorReplacements
                );

        newCreator.setMemberType(GroupMemberType.CREATOR);
        groupMemberRepository.save(newCreator);

        group.setCreatedBy(newCreator.getUser());
        readingGroupRepository.save(group);


        creatorMembership.setStatus(GroupMemberStatus.LEFT);

        groupMemberRepository.save(creatorMembership);
    }

    private GroupMemberEntity findReplacementCreator(
            ReadingGroupEntity group,
            List<GroupMemberEntity> candidates,
            Map<UUID, String> creatorReplacements
    ) {

        if (creatorReplacements != null) {

            String selectedUsername =
                    creatorReplacements.get(group.getIdExternal());

            if (selectedUsername != null
                    && !selectedUsername.isBlank()) {

                return candidates.stream()
                        .filter(member ->
                                member.getUser()
                                        .getProfileUsername()
                                        .equalsIgnoreCase(
                                                selectedUsername.trim()
                                        )
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Selected creator must be an active member of group: "
                                                + group.getName()
                                )
                        );
            }
        }

        return candidates.stream()
                .min(Comparator.comparing(GroupMemberEntity::getJoinedAt))
                .orElseThrow(() ->
                        new BadRequestException(
                                "Unable to assign a new creator for group: "
                                        + group.getName()
                        )
                );
    }

    private void processUserStories(
            UserEntity user,
            AccountStoryAction storyAction
    ) {

        if (storyAction == null) {
            throw new BadRequestException(
                    "A story action must be selected"
            );
        }

        if (storyAction == AccountStoryAction.KEEP) {
            return;
        }

        List<StoryEntity> activeStories =
                storyRepository.findByAuthorIdAndDeletedFalse(
                        user.getId()
                );

        activeStories.forEach(story ->
                story.setDeleted(true)
        );

        storyRepository.saveAll(activeStories);
    }
}