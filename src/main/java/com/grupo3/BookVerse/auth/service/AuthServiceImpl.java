package com.grupo3.BookVerse.auth.service;

import com.grupo3.BookVerse.auth.dtos.AuthRequest;
import com.grupo3.BookVerse.auth.dtos.AuthResponse;
import com.grupo3.BookVerse.auth.dtos.RegisterRequest;
import com.grupo3.BookVerse.auth.jwt.JwtService;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionEntity;
import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionType;
import com.grupo3.BookVerse.features.subscriptions.repository.SubscriptionRepository;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.users.dto.UserRequestDto;
import com.grupo3.BookVerse.features.users.dto.UserResponseDto;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import com.grupo3.BookVerse.features.users.service.UserService;
import com.grupo3.BookVerse.infrastructure.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final SubscriptionRepository subscriptionRepository;
    private final EmailService emailService;

    @Override
    @Transactional(readOnly = true)
    public AuthResponse authenticate(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        UserEntity user = userRepository.findByEmailWithRoles(request.email())
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found with email: " + request.email()
                        )
                );

        String token = jwtService.generateToken(user);

        return new AuthResponse(token, user.getIdExternal());
    }

    @Override
    @Transactional
    public UserResponseDto register(RegisterRequest request) {
        SubscriptionEntity freeSubscription =
                subscriptionRepository.findByType(SubscriptionType.FREE)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Default FREE subscription not found"
                                )
                        );

        UserRequestDto userRequestDto = new UserRequestDto(
                request.username(),
                request.email(),
                request.password(),
                freeSubscription.getIdExternal()
        );

        UserResponseDto registeredUser =
                userService.createUser(userRequestDto);

        emailService.sendWelcomeEmail(
                registeredUser.email(),
                registeredUser.username()
        );

        return registeredUser;
    }
}