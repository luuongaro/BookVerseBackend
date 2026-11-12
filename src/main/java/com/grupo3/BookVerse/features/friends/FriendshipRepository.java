package com.grupo3.BookVerse.features.friends;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Friendship f where f.userLow.id = :low and f.userHigh.id = :high")
    Optional<Friendship> findPairForUpdate(@Param("low") Long low, @Param("high") Long high);
    @Query("select f from Friendship f where f.userLow.id = :low and f.userHigh.id = :high")
    Optional<Friendship> findPair(@Param("low") Long low, @Param("high") Long high);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Friendship f where f.idExternal = :id")
    Optional<Friendship> findByExternalForUpdate(@Param("id") UUID id);
    @Query("select f from Friendship f where f.status = :status and (f.userLow.id = :userId or f.userHigh.id = :userId)")
    List<Friendship> findForUser(@Param("userId") Long userId, @Param("status") Friendship.Status status);
}
