package com.grupo3.BookVerse.features.users.repository;

import com.grupo3.BookVerse.features.users.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByIdExternal(UUID idExternal);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

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
