package com.grupo3.BookVerse.features.users.repository;

import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.users.domain.UserStatus;
import com.grupo3.BookVerse.features.users.dto.UserProfileResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.id = :id and u.status = com.grupo3.BookVerse.features.users.domain.UserStatus.ACTIVE")
    Optional<UserEntity> lockActiveUser(@Param("id") Long id);

    Optional<UserEntity> findByIdExternal(UUID idExternal);

    Optional<UserEntity> findByIdExternalAndStatus(UUID idExternal, UserStatus status);

    @Query("""
            SELECT new com.grupo3.BookVerse.features.users.dto.UserProfileResponseDto(u.idExternal, u.username, COALESCE(a.avatarIndex, 0))
            FROM UserEntity u
            LEFT JOIN ProfileAppearanceEntity a ON a.user.id = u.id
            WHERE u.status = :status AND u.idExternal <> :viewerId
              AND LOCATE(LOWER(:username), LOWER(u.username)) > 0
            ORDER BY LOWER(u.username), u.id
            """)
    Page<UserProfileResponseDto> searchProfiles(@Param("username") String username,
                                               @Param("status") UserStatus status,
                                               @Param("viewerId") UUID viewerId,
                                               Pageable pageable);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where lower(u.email) = lower(:email) and u.status = com.grupo3.BookVerse.features.users.domain.UserStatus.ACTIVE")
    Optional<UserEntity> findForPasswordRecoveryByEmail(@Param("email") String email);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.id = :id and u.status = com.grupo3.BookVerse.features.users.domain.UserStatus.ACTIVE")
    Optional<UserEntity> findForPasswordRecoveryById(@Param("id") Long id);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    @Query("""
            SELECT u
            FROM UserEntity u
            LEFT JOIN FETCH u.roles
            LEFT JOIN FETCH u.subscription
            WHERE UPPER(u.email) = UPPER(:email)
            """)
    Optional<UserEntity> findByEmailWithRoles(@Param("email") String email);

    @Query("""
        SELECT u
        FROM UserEntity u
        LEFT JOIN FETCH u.subscription
        WHERE u.id = :id
        """)
    Optional<UserEntity> findByIdWithSubscription(@Param("id") Long id);
}
