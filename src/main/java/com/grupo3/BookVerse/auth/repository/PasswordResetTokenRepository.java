package com.grupo3.BookVerse.auth.repository;

import com.grupo3.BookVerse.auth.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByUserId(Long userId);

    // Read only the ID before locking the user. Both issuance and consumption
    // acquire the user lock first, so concurrent requests cannot reuse a token.
    @Query("select t.user.id from PasswordResetToken t where t.tokenHash = :hash")
    Optional<Long> findUserIdByTokenHash(@Param("hash") String hash);
}
