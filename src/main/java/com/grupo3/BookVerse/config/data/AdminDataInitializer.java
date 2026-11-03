package com.grupo3.BookVerse.config.data;

import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.roles.domain.RoleEntity;
import com.grupo3.BookVerse.features.roles.repository.RoleRepository;
import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionEntity;
import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionType;
import com.grupo3.BookVerse.features.subscriptions.repository.SubscriptionRepository;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.users.domain.UserStatus;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(3)
public class AdminDataInitializer implements CommandLineRunner {

    private static final String ADMIN_ROLE = "ROLE_ADMIN";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        createAdminIfNotExists();
    }

    private void createAdminIfNotExists() {
        RoleEntity adminRole =
                roleRepository.findByNameIgnoreCase(ADMIN_ROLE)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Role not found: " + ADMIN_ROLE
                                )
                        );

        UserEntity existingAdmin =
                userRepository.findByEmailWithRoles(adminEmail)
                        .orElse(null);

        if (existingAdmin != null) {
            boolean changed = false;

            boolean hasAdminRole =
                    existingAdmin.getRoles().stream()
                            .anyMatch(role ->
                                    ADMIN_ROLE.equalsIgnoreCase(
                                            role.getName()
                                    )
                            );

            if (!hasAdminRole) {
                existingAdmin.getRoles().add(adminRole);
                changed = true;

                log.warn(
                        "ROLE_ADMIN restored for protected administrator: {}",
                        adminEmail
                );
            }

            if (existingAdmin.getStatus() != UserStatus.ACTIVE) {
                existingAdmin.setStatus(UserStatus.ACTIVE);
                changed = true;

                log.warn(
                        "ACTIVE status restored for protected administrator: {}",
                        adminEmail
                );
            }

            if (changed) {
                userRepository.save(existingAdmin);
            } else {
                log.info(
                        "Protected administrator already configured correctly: {}",
                        adminEmail
                );
            }

            return;
        }

        SubscriptionEntity adminSubscription =
                subscriptionRepository
                        .findByType(SubscriptionType.PREMIUM)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Subscription not found: PREMIUM"
                                )
                        );

        UserEntity adminUser = UserEntity.builder()
                .username(adminUsername)
                .email(adminEmail.trim().toLowerCase())
                .passwordHash(
                        passwordEncoder.encode(adminPassword)
                )
                .subscription(adminSubscription)
                .status(UserStatus.ACTIVE)
                .build();

        adminUser.getRoles().add(adminRole);

        userRepository.save(adminUser);

        log.info(
                "Protected administrator created with email: {}",
                adminEmail
        );
    }
}
