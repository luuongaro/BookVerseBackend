package com.grupo3.BookVerse.features.groups.groupMember.repository;

import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberEntity;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberStatus;
import com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupMemberRepository extends JpaRepository<GroupMemberEntity, Long> {

    @Query("SELECT COUNT(m) FROM GroupMemberEntity m WHERE m.user.id = :userId AND m.status = com.grupo3.BookVerse.features.groups.groupMember.domain.GroupMemberStatus.ACTIVE AND m.group.isActive = true")
    long countActiveGroupsByUser(@Param("userId") Long userId);

    Optional<GroupMemberEntity> findByIdExternal(UUID idExternal);

    Optional<GroupMemberEntity> findByGroupIdExternalAndUserIdExternal(UUID groupId, UUID userId);

    List<GroupMemberEntity> findByGroupIdExternalAndStatus(UUID groupId, GroupMemberStatus status);

    List<GroupMemberEntity> findByUserIdExternalAndStatus(UUID userId, GroupMemberStatus status);

    boolean existsByGroupIdExternalAndUserIdExternalAndStatus(
            UUID groupId,
            UUID userId,
            GroupMemberStatus status
    );

    boolean existsByGroupIdExternalAndUserIdExternalAndMemberTypeAndStatus(
            UUID groupId,
            UUID userId,
            GroupMemberType memberType,
            GroupMemberStatus status
    );

    long countByGroupIdExternalAndStatus(
            UUID groupId,
            GroupMemberStatus status
    );
}
