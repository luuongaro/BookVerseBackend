package com.grupo3.BookVerse.features.groups.groupCommentReport.domain;

import com.grupo3.BookVerse.features.groups.groupComment.domain.GroupCommentEntity;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "group_comment_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupCommentReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "id_external",
            nullable = false,
            unique = true,
            updatable = false
    )
    private UUID idExternal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "comment_id",
            nullable = false
    )
    private GroupCommentEntity comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "reporter_user_id",
            nullable = false
    )
    private UserEntity reporterUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderator_user_id")
    private UserEntity moderatorUser;

    @Column(
            name = "reason",
            nullable = false,
            length = 1000
    )
    private String reason;

    @Column(
            name = "details",
            columnDefinition = "TEXT"
    )
    private String details;

    @Column(
            name = "resolution_comment",
            columnDefinition = "TEXT"
    )
    private String resolutionComment;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false
    )
    private GroupCommentReportStatus status;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        if (idExternal == null) {
            idExternal = UUID.randomUUID();
        }

        if (status == null) {
            status = GroupCommentReportStatus.PENDING;
        }

        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}