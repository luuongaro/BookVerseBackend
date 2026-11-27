package com.grupo3.BookVerse.features.users.repository;

import com.grupo3.BookVerse.features.users.domain.FavoriteBookEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface FavoriteBookRepository extends JpaRepository<FavoriteBookEntity, Long> {

    Optional<FavoriteBookEntity> findByUserIdExternalAndBookIdExternal(
            UUID userId,
            UUID bookId
    );

    @Query(
            value = """
                    SELECT f FROM FavoriteBookEntity f
                    JOIN FETCH f.book b
                    WHERE f.user.idExternal = :userId
                      AND b.deleted = false
                    ORDER BY f.createdAt DESC, f.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(f) FROM FavoriteBookEntity f
                    WHERE f.user.idExternal = :userId
                      AND f.book.deleted = false
                    """
    )
    Page<FavoriteBookEntity> findVisibleFavorites(
            @Param("userId") UUID userId,
            Pageable pageable
    );
}