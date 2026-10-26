package com.grupo3.BookVerse.features.users.service.impl;

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
import com.grupo3.BookVerse.features.users.dto.UserUpdateRequestDto;
import com.grupo3.BookVerse.features.users.mappers.UserMapper;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateUserSuccessfully() {
        UUID subscriptionId = UUID.randomUUID();

        UserRequestDto request = new UserRequestDto(
                "reader",
                "reader@mail.com",
                "Password1.",
                subscriptionId
        );

        SubscriptionEntity subscription = SubscriptionEntity.builder()
                .idExternal(subscriptionId)
                .build();

        RoleEntity defaultRole = RoleEntity.builder()
                .name("ROLE_USER")
                .build();

        UserEntity mappedUser = UserEntity.builder()
                .username("reader")
                .email("reader@mail.com")
                .build();

        UserEntity savedUser = UserEntity.builder()
                .id(1L)
                .idExternal(UUID.randomUUID())
                .username("reader")
                .email("reader@mail.com")
                .passwordHash("encoded-password")
                .subscription(subscription)
                .status(UserStatus.ACTIVE)
                .build();

        UserResponseDto response = new UserResponseDto(
                savedUser.getIdExternal(),
                "reader",
                "reader@mail.com",
                "ACTIVE",
                List.of("ROLE_USER")
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(subscriptionRepository.findByIdExternal(subscriptionId)).thenReturn(Optional.of(subscription));
        when(roleRepository.findByNameIgnoreCase("ROLE_USER")).thenReturn(Optional.of(defaultRole));
        when(userMapper.toEntity(request)).thenReturn(mappedUser);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);
        when(userMapper.toResponseDto(savedUser)).thenReturn(response);

        UserResponseDto result = userService.createUser(request);

        assertNotNull(result);
        assertEquals("reader", result.username());
        assertEquals("reader@mail.com", result.email());

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());

        UserEntity userToSave = userCaptor.getValue();
        assertEquals("encoded-password", userToSave.getPasswordHash());
        assertEquals(subscription, userToSave.getSubscription());
        assertTrue(userToSave.getRoles().contains(defaultRole));
    }

    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        UserRequestDto request = buildUserRequest();

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> userService.createUser(request)
        );

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void shouldThrowExceptionWhenUsernameAlreadyExists() {
        UserRequestDto request = buildUserRequest();

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByUsername(request.username())).thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> userService.createUser(request)
        );

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void shouldThrowExceptionWhenSubscriptionDoesNotExist() {
        UUID subscriptionId = UUID.randomUUID();

        UserRequestDto request = new UserRequestDto(
                "reader",
                "reader@mail.com",
                "Password1.",
                subscriptionId
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(subscriptionRepository.findByIdExternal(subscriptionId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.createUser(request)
        );

        verify(roleRepository, never()).findByNameIgnoreCase(any());
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void shouldThrowExceptionWhenDefaultRoleDoesNotExist() {
        UUID subscriptionId = UUID.randomUUID();

        UserRequestDto request = new UserRequestDto(
                "reader",
                "reader@mail.com",
                "Password1.",
                subscriptionId
        );

        SubscriptionEntity subscription = SubscriptionEntity.builder()
                .idExternal(subscriptionId)
                .build();

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(subscriptionRepository.findByIdExternal(subscriptionId)).thenReturn(Optional.of(subscription));
        when(roleRepository.findByNameIgnoreCase("ROLE_USER")).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.createUser(request)
        );

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void shouldReturnAllUsers() {
        UserEntity user = UserEntity.builder()
                .username("reader")
                .email("reader@mail.com")
                .build();

        UserResponseDto response = new UserResponseDto(
                UUID.randomUUID(),
                "reader",
                "reader@mail.com",
                "ACTIVE",
                List.of("ROLE_USER")
        );

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userMapper.toResponseDtoList(List.of(user))).thenReturn(List.of(response));

        List<UserResponseDto> result = userService.getAllUsers();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("reader", result.get(0).username());

        verify(userRepository).findAll();
        verify(userMapper).toResponseDtoList(List.of(user));
    }

    @Test
    void shouldReturnUserByIdExternalWhenAuthenticatedUserIsSelf() {
        UUID idExternal = UUID.randomUUID();

        UserEntity targetUser = UserEntity.builder()
                .id(1L)
                .idExternal(idExternal)
                .username("reader")
                .email("reader@mail.com")
                .build();

        UserResponseDto response = new UserResponseDto(
                idExternal,
                "reader",
                "reader@mail.com",
                "ACTIVE",
                List.of("ROLE_USER")
        );

        authenticateAs(targetUser);

        when(userRepository.findByIdExternal(idExternal)).thenReturn(Optional.of(targetUser));
        when(userMapper.toResponseDto(targetUser)).thenReturn(response);

        UserResponseDto result = userService.getUserByIdExternal(idExternal);

        assertNotNull(result);
        assertEquals(idExternal, result.idExternal());

        verify(userMapper).toResponseDto(targetUser);
    }

    @Test
    void shouldDenyUserViewWhenAuthenticatedUserIsNotSelfAdminOrModerator() {
        UUID idExternal = UUID.randomUUID();

        UserEntity targetUser = UserEntity.builder()
                .id(2L)
                .idExternal(idExternal)
                .username("target")
                .email("target@mail.com")
                .build();

        UserEntity authenticatedUser = UserEntity.builder()
                .id(1L)
                .username("reader")
                .email("reader@mail.com")
                .build();

        authenticatedUser.getRoles().add(RoleEntity.builder().name("ROLE_USER").build());
        authenticateAs(authenticatedUser);

        when(userRepository.findByIdExternal(idExternal)).thenReturn(Optional.of(targetUser));

        assertThrows(
                AccessDeniedException.class,
                () -> userService.getUserByIdExternal(idExternal)
        );

        verify(userMapper, never()).toResponseDto(any(UserEntity.class));
    }

    @Test
    void shouldUpdateUserWhenAuthenticatedUserIsSelf() {
        UUID idExternal = UUID.randomUUID();

        UserEntity existingUser = UserEntity.builder()
                .id(1L)
                .idExternal(idExternal)
                .username("oldname")
                .email("old@mail.com")
                .build();

        UserUpdateRequestDto request = new UserUpdateRequestDto(
                "newname",
                "new@mail.com"
        );

        UserResponseDto response = new UserResponseDto(
                idExternal,
                "newname",
                "new@mail.com",
                "ACTIVE",
                List.of("ROLE_USER")
        );

        authenticateAs(existingUser);

        when(userRepository.findByIdExternal(idExternal)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        doAnswer(invocation -> {
            UserUpdateRequestDto dto = invocation.getArgument(0);
            UserEntity entity = invocation.getArgument(1);

            entity.setUsername(dto.username());
            entity.setEmail(dto.email());

            return null;
        }).when(userMapper).updateEntityFromDto(request, existingUser);
        when(userRepository.save(existingUser)).thenReturn(existingUser);
        when(userMapper.toResponseDto(existingUser)).thenReturn(response);

        UserResponseDto result = userService.updateUser(idExternal, request);

        assertNotNull(result);
        assertEquals("newname", result.username());
        assertEquals("new@mail.com", result.email());

        verify(userRepository).save(existingUser);
        verify(userMapper).updateEntityFromDto(request, existingUser);
    }

    @Test
    void shouldDeleteUserWhenAuthenticatedUserIsAdmin() {
        UUID idExternal = UUID.randomUUID();

        UserEntity targetUser = UserEntity.builder()
                .id(2L)
                .idExternal(idExternal)
                .username("target")
                .email("target@mail.com")
                .status(UserStatus.ACTIVE)
                .build();

        UserEntity adminUser = UserEntity.builder()
                .id(1L)
                .username("admin")
                .email("admin@mail.com")
                .build();

        adminUser.getRoles().add(RoleEntity.builder().name("ROLE_ADMIN").build());
        authenticateAs(adminUser);

        when(userRepository.findByIdExternal(idExternal)).thenReturn(Optional.of(targetUser));

        userService.deleteUser(idExternal);

        assertEquals(UserStatus.INACTIVE, targetUser.getStatus());

        verify(userRepository).save(targetUser);
    }

    private UserRequestDto buildUserRequest() {
        return new UserRequestDto(
                "reader",
                "reader@mail.com",
                "Password1.",
                UUID.randomUUID()
        );
    }

    private void authenticateAs(UserEntity user) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        user.getAuthorities()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
