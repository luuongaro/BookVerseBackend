package com.grupo3.BookVerse.features.groups.groupMembershipRequest.repository;

import com.grupo3.BookVerse.features.groups.groupMembershipRequest.domain.GroupMembershipRequestEntity;
import com.grupo3.BookVerse.features.groups.groupMembershipRequest.domain.GroupMembershipRequestStatus;
import com.grupo3.BookVerse.features.groups.groupMembershipRequest.domain.GroupMembershipRequestType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupMembershipRequestRepository
        extends JpaRepository<GroupMembershipRequestEntity, Long> {

    Optional<GroupMembershipRequestEntity> findByIdExternal(
            UUID idExternal
    );

    boolean existsByGroup_IdExternalAndTargetUser_IdExternalAndTypeAndStatus(
            UUID groupId,
            UUID targetUserId,
            GroupMembershipRequestType type,
            GroupMembershipRequestStatus status
    );

    List<GroupMembershipRequestEntity>
    findByGroup_IdExternalAndTypeAndStatusOrderByCreatedAtAsc(
            UUID groupId,
            GroupMembershipRequestType type,
            GroupMembershipRequestStatus status
    );

    List<GroupMembershipRequestEntity>
    findByTargetUser_IdExternalAndTypeAndStatusOrderByCreatedAtDesc(
            UUID targetUserId,
            GroupMembershipRequestType type,
            GroupMembershipRequestStatus status
    );
}