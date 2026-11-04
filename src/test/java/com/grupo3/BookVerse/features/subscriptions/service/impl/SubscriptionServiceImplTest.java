package com.grupo3.BookVerse.features.subscriptions.service.impl;

import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionEntity;
import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionType;
import com.grupo3.BookVerse.features.subscriptions.dto.SubscriptionResponseDto;
import com.grupo3.BookVerse.features.subscriptions.mappers.SubscriptionMapper;
import com.grupo3.BookVerse.features.subscriptions.repository.SubscriptionRepository;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionMapper subscriptionMapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private UUID subscriptionId;
    private SubscriptionEntity subscription;
    private SubscriptionResponseDto response;
    private UserEntity user;

    @BeforeEach
    void setUp() {

        subscriptionId = UUID.randomUUID();

        subscription = new SubscriptionEntity();
        subscription.setId(1L);
        subscription.setIdExternal(subscriptionId);
        subscription.setType(SubscriptionType.PREMIUM);

        response = SubscriptionResponseDto.builder()
                .idExternal(subscriptionId)
                .type("PREMIUM")
                .build();

        user = new UserEntity();
        user.setId(1L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllSubscriptions_shouldReturnList() {

        List<SubscriptionEntity> entities =
                List.of(subscription);

        List<SubscriptionResponseDto> responses =
                List.of(response);

        when(subscriptionRepository.findAll())
                .thenReturn(entities);

        when(subscriptionMapper.toResponseDtoList(entities))
                .thenReturn(responses);

        List<SubscriptionResponseDto> result =
                subscriptionService.getAllSubscriptions();

        assertEquals(1, result.size());
        assertEquals(response, result.get(0));

        verify(subscriptionRepository)
                .findAll();
    }

    @Test
    void getSubscriptionByIdExternal_shouldReturnResponse() {

        when(subscriptionRepository.findByIdExternal(subscriptionId))
                .thenReturn(Optional.of(subscription));

        when(subscriptionMapper.toResponseDto(subscription))
                .thenReturn(response);

        SubscriptionResponseDto result =
                subscriptionService.getSubscriptionByIdExternal(subscriptionId);

        assertEquals(response, result);

        verify(subscriptionRepository)
                .findByIdExternal(subscriptionId);
    }

    @Test
    void getSubscriptionByIdExternal_shouldThrow_whenNotFound() {

        when(subscriptionRepository.findByIdExternal(subscriptionId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> subscriptionService.getSubscriptionByIdExternal(subscriptionId)
        );

        verify(subscriptionMapper, never())
                .toResponseDto(any());
    }

    @Test
    void getAuthenticatedUserSubscription_shouldReturnSubscription() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);

        user.setSubscription(subscription);
        when(subscriptionMapper.toResponseDto(subscription)).thenReturn(response);

        SubscriptionResponseDto result =
                subscriptionService.getAuthenticatedUserSubscription();

        assertEquals(response, result);
        verify(subscriptionMapper).toResponseDto(subscription);
    }

    @Test
    void getAuthenticatedUserSubscription_shouldThrow_whenUserNotAuthenticated() {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        assertThrows(
                AccessDeniedException.class,
                () -> subscriptionService.getAuthenticatedUserSubscription()
        );
    }

    @Test
    void getAuthenticatedUserSubscription_shouldThrow_whenPrincipalIsNotUserEntity() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn("not-a-user");
        SecurityContextHolder.setContext(securityContext);

        assertThrows(
                AccessDeniedException.class,
                () -> subscriptionService.getAuthenticatedUserSubscription()
        );
    }
}
