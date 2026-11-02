package com.grupo3.BookVerse.features.users.repository;

import com.grupo3.BookVerse.features.users.domain.FavoriteStoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface FavoriteStoryRepository extends JpaRepository<FavoriteStoryEntity, Long> {
    Optional<FavoriteStoryEntity> findByUserIdExternalAndStoryIdExternal(UUID userId, UUID storyId);

    @Query(value = """
            SELECT f FROM FavoriteStoryEntity f JOIN FETCH f.story s
            WHERE f.user.idExternal = :userId AND s.deleted = false AND s.hidden = false
            ORDER BY f.createdAt DESC, f.id DESC
            """, countQuery = """
            SELECT COUNT(f) FROM FavoriteStoryEntity f
            WHERE f.user.idExternal = :userId AND f.story.deleted = false AND f.story.hidden = false
            """)
    Page<FavoriteStoryEntity> findVisibleFavorites(@Param("userId") UUID userId, Pageable pageable);
}
