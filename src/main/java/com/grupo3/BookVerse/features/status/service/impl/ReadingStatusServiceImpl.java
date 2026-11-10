package com.grupo3.BookVerse.features.status.service.impl;
import com.grupo3.BookVerse.features.groups.groupReadingParticipant.service.GroupReadingParticipantService;
import com.grupo3.BookVerse.features.status.service.ReadingStatusService;
import com.grupo3.BookVerse.features.stories.domain.StoryAccessType;
import com.grupo3.BookVerse.features.stories.domain.StoryEntity;
import com.grupo3.BookVerse.features.stories.repository.StoryRepository;
import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionType;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.books.domain.BookEntity;
import com.grupo3.BookVerse.features.books.repository.BookRepository;
import com.grupo3.BookVerse.features.chapters.repository.ChapterRepository;
import com.grupo3.BookVerse.features.status.domain.ProgressType;
import com.grupo3.BookVerse.features.status.domain.ReadingStatusEntity;
import com.grupo3.BookVerse.features.status.domain.ReadingStatusEnum;
import com.grupo3.BookVerse.features.status.dto.ReadingStatusRequestDto;
import com.grupo3.BookVerse.features.status.dto.ReadingStatusResponseDto;
import com.grupo3.BookVerse.features.status.mappers.ReadingStatusMapper;
import com.grupo3.BookVerse.features.status.repository.ReadingStatusRepository;

import java.util.List;
import java.util.UUID;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReadingStatusServiceImpl implements ReadingStatusService {

    private static final List<ReadingStatusEnum> ACTIVE_STORY_READING_STATUSES = List.of(
            ReadingStatusEnum.IN_PROGRESS,
            ReadingStatusEnum.RE_READING
    );

    private final ReadingStatusRepository readingStatusRepository;
    private final ReadingStatusMapper readingStatusMapper;
    private final GroupReadingParticipantService groupReadingParticipantService;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;

    @Override
    @Transactional
    public ReadingStatusResponseDto createReadingStatus(
            ReadingStatusRequestDto requestDto
    ) {

        validateContentAssociation(requestDto);
        validateProgress(requestDto);
        validateProgressConsistency(requestDto);

        UserEntity authenticatedUser = getAuthenticatedUserWithSubscription();

        BookEntity book = null;
        StoryEntity story = null;

        if (requestDto.bookId() != null) {
            book = findBookByIdExternal(requestDto.bookId());
        }

        if (requestDto.storyId() != null) {
            story = findStoryByIdExternal(requestDto.storyId());
            validateStoryReadingRules(
                    authenticatedUser,
                    story,
                    requestDto.status(),
                    null
            );
        }

        validateUniqueReadingStatus(
                authenticatedUser,
                book,
                story,
                null
        );

        ReadingStatusEntity entity = readingStatusMapper.toEntity(requestDto);
        entity.setUser(authenticatedUser);
        entity.setBook(book);
        entity.setStory(story);

        ReadingStatusEntity saved =
                readingStatusRepository.save(entity);

        groupReadingParticipantService
                .synchronizeWithReadingStatus(saved);

        return readingStatusMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ReadingStatusResponseDto getReadingStatusByIdExternal(
            UUID idExternal
    ) {

        ReadingStatusEntity entity = findReadingStatusByIdExternal(idExternal);
        UserEntity authenticatedUser = getAuthenticatedUser();

        validateCanAccessReadingStatus(entity, authenticatedUser);

        return readingStatusMapper.toResponseDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadingStatusResponseDto> getAllReadingStatuses() {

        UserEntity authenticatedUser = getAuthenticatedUser();

        validateAdminOrModerator(authenticatedUser);

        List<ReadingStatusEntity> readingStatuses =
                readingStatusRepository.findAll();

        return readingStatusMapper.toResponseDtoList(readingStatuses);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadingStatusResponseDto> getMyReadingStatuses() {
        UserEntity authenticatedUser = getAuthenticatedUser();

        List<ReadingStatusEntity> readingStatuses =
                readingStatusRepository.findByUserIdExternal(
                        authenticatedUser.getIdExternal()
                );

        return readingStatusMapper.toResponseDtoList(readingStatuses);
    }

    @Override
    @Transactional
    public void updateShelfOrder(List<UUID> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > 1000 || ids.stream().anyMatch(java.util.Objects::isNull) || new HashSet<>(ids).size() != ids.size()) {
            throw new BadRequestException("Invalid shelf order");
        }
        UserEntity user = getAuthenticatedUser();
        Map<UUID, ReadingStatusEntity> owned = readingStatusRepository.findByUserIdExternal(user.getIdExternal())
                .stream().collect(Collectors.toMap(ReadingStatusEntity::getIdExternal, Function.identity()));
        if (!owned.keySet().containsAll(ids)) throw new AccessDeniedException("Reading status is not owned by the authenticated user");
        ReadingStatusEntity first = owned.get(ids.get(0));
        boolean books = first.getBook() != null;
        if (ids.stream().map(owned::get).anyMatch(item -> item.getStatus() != first.getStatus() || (item.getBook() != null) != books)) {
            throw new BadRequestException("All readings must belong to the same shelf");
        }
        for (int index = 0; index < ids.size(); index++) owned.get(ids.get(index)).setShelfPosition(index);
        readingStatusRepository.saveAll(ids.stream().map(owned::get).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadingStatusResponseDto> getReadingStatusesByUser(
            UUID userId
    ) {

        UserEntity authenticatedUser = getAuthenticatedUser();
        UserEntity targetUser = findUserByIdExternal(userId);

        validateCanAccessUserReadingStatuses(targetUser, authenticatedUser);

        List<ReadingStatusEntity> readingStatuses =
                readingStatusRepository.findByUserIdExternal(userId);

        return readingStatusMapper.toResponseDtoList(readingStatuses);
    }

    @Override
    @Transactional
    public ReadingStatusResponseDto updateReadingStatus(
            UUID idExternal,
            ReadingStatusRequestDto requestDto
    ) {

        validateContentAssociation(requestDto);
        validateProgress(requestDto);
        validateProgressConsistency(requestDto);

        ReadingStatusEntity entity = findReadingStatusByIdExternal(idExternal);
        UserEntity authenticatedUser = getAuthenticatedUserWithSubscription();

        validateCanModifyReadingStatus(entity, authenticatedUser);

        boolean contentChanged =
                !java.util.Objects.equals(
                        entity.getBook() == null
                                ? null
                                : entity.getBook().getIdExternal(),
                        requestDto.bookId()
                )
                        || !java.util.Objects.equals(
                        entity.getStory() == null
                                ? null
                                : entity.getStory().getIdExternal(),
                        requestDto.storyId()
                );

        if (contentChanged) {
            groupReadingParticipantService
                    .withdrawForDeletedReadingStatus(entity);
        }

        BookEntity book = null;
        StoryEntity story = null;

        if (requestDto.bookId() != null) {
            book = findBookByIdExternal(requestDto.bookId());
        }

        if (requestDto.storyId() != null) {
            story = findStoryByIdExternal(requestDto.storyId());
            validateStoryReadingRules(
                    authenticatedUser,
                    story,
                    requestDto.status(),
                    entity.getIdExternal()
            );
        }

        validateUniqueReadingStatus(
                authenticatedUser,
                book,
                story,
                entity.getIdExternal()
        );

        if (entity.getStatus() != requestDto.status() || !java.util.Objects.equals(
                entity.getBook() == null ? null : entity.getBook().getIdExternal(), requestDto.bookId()) || !java.util.Objects.equals(
                entity.getStory() == null ? null : entity.getStory().getIdExternal(), requestDto.storyId())) {
            entity.setShelfPosition(null);
        }
        entity.setUser(authenticatedUser);
        entity.setBook(book);
        entity.setStory(story);
        entity.setStatus(requestDto.status());
        entity.setProgressType(requestDto.progressType());
        entity.setProgressValue(requestDto.progressValue());
        entity.setProgressTotal(requestDto.progressTotal());
        entity.setStartedAt(requestDto.startedAt());
        entity.setFinishedAt(requestDto.finishedAt());

        ReadingStatusEntity saved =
                readingStatusRepository.save(entity);

        groupReadingParticipantService
                .synchronizeWithReadingStatus(saved);

        return readingStatusMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public void deleteReadingStatus(UUID idExternal) {

        ReadingStatusEntity entity = findReadingStatusByIdExternal(idExternal);
        UserEntity authenticatedUser = getAuthenticatedUser();

        validateCanModifyReadingStatus(entity, authenticatedUser);

        groupReadingParticipantService
                .withdrawForDeletedReadingStatus(entity);

        readingStatusRepository.delete(entity);
    }

    private void validateContentAssociation(
            ReadingStatusRequestDto requestDto
    ) {

        boolean hasBook = requestDto.bookId() != null;
        boolean hasStory = requestDto.storyId() != null;

        if (hasBook == hasStory) {
            throw new BadRequestException(
                    "A reading status must be associated with either a book or a story"
            );
        }
    }

    private void validateProgress(
            ReadingStatusRequestDto requestDto
    ) {

        boolean hasType = requestDto.progressType() != null;
        boolean hasValue = requestDto.progressValue() != null;

        if (hasType != hasValue) {
            throw new BadRequestException(
                    "progressType and progressValue must both be provided or both be null"
            );
        }

        if (!hasType) {

            if (requestDto.progressTotal() != null) {
                throw new BadRequestException(
                        "progressTotal cannot be provided without reading progress"
                );
            }

            return;
        }

        if (requestDto.progressValue() <= 0) {
            throw new BadRequestException(
                    "progressValue must be greater than 0"
            );
        }

        if (requestDto.progressType() == ProgressType.PERCENTAGE
                && requestDto.progressValue() > 100) {
            throw new BadRequestException(
                    "Percentage progress cannot exceed 100"
            );
        }

        validateProgressTotal(requestDto);
    }

    private void validateProgressTotal(
            ReadingStatusRequestDto requestDto
    ) {

        boolean isBook =
                requestDto.bookId() != null;

        boolean isChapterProgress =
                requestDto.progressType() == ProgressType.CHAPTER;

        if (isBook && isChapterProgress) {

            if (requestDto.progressTotal() == null) {
                throw new BadRequestException(
                        "progressTotal is required when tracking book progress by chapter"
                );
            }

            if (requestDto.progressTotal() <= 0) {
                throw new BadRequestException(
                        "progressTotal must be greater than 0"
                );
            }

            if (requestDto.progressValue() > requestDto.progressTotal()) {
                throw new BadRequestException(
                        "Current chapter cannot exceed total chapters"
                );
            }

            return;
        }

        if (requestDto.progressTotal() != null) {
            throw new BadRequestException(
                    "progressTotal is only allowed for chapter-based book progress"
            );
        }
    }

    private void validateProgressConsistency(
            ReadingStatusRequestDto requestDto
    ) {
        boolean hasProgress =
                requestDto.progressType() != null
                        && requestDto.progressValue() != null;

        if (requestDto.status() == ReadingStatusEnum.WANT_TO_READ
                && hasProgress) {
            throw new BadRequestException(
                    "WANT_TO_READ cannot have reading progress"
            );
        }

        if (requestDto.status() == ReadingStatusEnum.FINISHED
                && !hasProgress) {
            throw new BadRequestException(
                    "FINISHED must include reading progress"
            );
        }

        if (!hasProgress) {
            return;
        }

        if (requestDto.progressType() == ProgressType.PERCENTAGE) {
            validatePercentageConsistency(requestDto);
            return;
        }

        validateChapterConsistency(requestDto);
    }

    private void validateStoryReadingRules(
            UserEntity user,
            StoryEntity story,
            ReadingStatusEnum status,
            UUID currentReadingStatusId
    ) {
        if (user.getSubscription() == null) {
            throw new AccessDeniedException("User subscription not found");
        }

        boolean isFreeUser =
                user.getSubscription().getType() == SubscriptionType.FREE;

        boolean isPremiumStory =
                story.getAccessType() == StoryAccessType.PREMIUM;

        if (isFreeUser && isPremiumStory) {
            throw new AccessDeniedException(
                    "Free users can only create or update reading statuses for free stories"
            );
        }

        boolean isOwnStory =
                story.getAuthor() != null
                        && story.getAuthor().getId().equals(user.getId());

        boolean isActiveStoryReading =
                ACTIVE_STORY_READING_STATUSES.contains(status);

        if (!isFreeUser || !isActiveStoryReading || isOwnStory) {
            return;
        }

        int maxActiveStoriesReading =
                user.getSubscription().getMaxActiveStoriesReading();

        long currentActiveStories =
                currentReadingStatusId == null
                        ? readingStatusRepository.countDistinctActiveStoriesByUserExcludingOwnStories(
                        user.getIdExternal(),
                        ACTIVE_STORY_READING_STATUSES
                )
                        : readingStatusRepository.countDistinctActiveStoriesByUserExcludingOwnStoriesAndCurrentStatus(
                        user.getIdExternal(),
                        ACTIVE_STORY_READING_STATUSES,
                        currentReadingStatusId
                );

        boolean sameStoryAlreadyActive =
                currentReadingStatusId == null
                        ? readingStatusRepository.existsActiveReadingByUserAndStory(
                        user.getIdExternal(),
                        story.getIdExternal(),
                        ACTIVE_STORY_READING_STATUSES
                )
                        : readingStatusRepository.existsActiveReadingByUserAndStoryAndCurrentStatusNot(
                        user.getIdExternal(),
                        story.getIdExternal(),
                        ACTIVE_STORY_READING_STATUSES,
                        currentReadingStatusId
                );

        if (!sameStoryAlreadyActive && currentActiveStories >= maxActiveStoriesReading) {
            throw new BadRequestException(
                    "You have reached the maximum number of active stories allowed by your current subscription"
            );
        }
    }

    private ReadingStatusEntity findReadingStatusByIdExternal(
            UUID idExternal
    ) {

        return readingStatusRepository.findByIdExternal(idExternal)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Reading status not found with idExternal: "
                                        + idExternal
                        )
                );
    }

    private UserEntity findUserByIdExternal(
            UUID idExternal
    ) {

        return userRepository.findByIdExternal(idExternal)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with idExternal: "
                                        + idExternal
                        )
                );
    }

    private BookEntity findBookByIdExternal(
            UUID idExternal
    ) {

        return bookRepository.findByIdExternal(idExternal)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Book not found with idExternal: "
                                        + idExternal
                        )
                );
    }

    private StoryEntity findStoryByIdExternal(
            UUID idExternal
    ) {

        return storyRepository.findByIdExternalAndDeletedFalse(idExternal)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Story not found with idExternal: "
                                        + idExternal
                        )
                );
    }

    private UserEntity getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserEntity user)) {
            throw new AccessDeniedException("Authenticated user not found");
        }

        return user;
    }

    private void validateCanAccessReadingStatus(
            ReadingStatusEntity entity,
            UserEntity authenticatedUser
    ) {
        boolean isOwner =
                entity.getUser() != null
                        && entity.getUser().getId().equals(authenticatedUser.getId());

        boolean isAdmin = authenticatedUser.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        boolean isModerator = authenticatedUser.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_MODERATOR".equals(authority.getAuthority()));

        if (!isOwner && !isAdmin && !isModerator) {
            throw new AccessDeniedException(
                    "You do not have permission to access this reading status"
            );
        }
    }

    private void validateCanModifyReadingStatus(
            ReadingStatusEntity entity,
            UserEntity authenticatedUser
    ) {
        boolean isOwner =
                entity.getUser() != null
                        && entity.getUser().getId().equals(authenticatedUser.getId());

        boolean isAdmin = authenticatedUser.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException(
                    "You do not have permission to modify this reading status"
            );
        }
    }

    private void validateCanAccessUserReadingStatuses(
            UserEntity targetUser,
            UserEntity authenticatedUser
    ) {
        boolean isSelf =
                targetUser.getId().equals(authenticatedUser.getId());

        boolean isAdmin = authenticatedUser.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        boolean isModerator = authenticatedUser.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_MODERATOR".equals(authority.getAuthority()));

        if (!isSelf && !isAdmin && !isModerator) {
            throw new AccessDeniedException(
                    "You do not have permission to access these reading statuses"
            );
        }
    }

    private void validateAdminOrModerator(UserEntity authenticatedUser) {
        boolean isAdmin = authenticatedUser.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        boolean isModerator = authenticatedUser.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_MODERATOR".equals(authority.getAuthority()));

        if (!isAdmin && !isModerator) {
            throw new AccessDeniedException(
                    "You do not have permission to access all reading statuses"
            );
        }
    }

    private void validateUniqueReadingStatus(
            UserEntity user,
            BookEntity book,
            StoryEntity story,
            UUID currentReadingStatusId
    ) {

        boolean exists;

        if (book != null) {

            exists = currentReadingStatusId == null
                    ? readingStatusRepository
                    .existsByUser_IdExternalAndBook_IdExternal(
                            user.getIdExternal(),
                            book.getIdExternal()
                    )
                    : readingStatusRepository
                    .existsByUser_IdExternalAndBook_IdExternalAndIdExternalNot(
                            user.getIdExternal(),
                            book.getIdExternal(),
                            currentReadingStatusId
                    );

        } else {

            exists = currentReadingStatusId == null
                    ? readingStatusRepository
                    .existsByUser_IdExternalAndStory_IdExternal(
                            user.getIdExternal(),
                            story.getIdExternal()
                    )
                    : readingStatusRepository
                    .existsByUser_IdExternalAndStory_IdExternalAndIdExternalNot(
                            user.getIdExternal(),
                            story.getIdExternal(),
                            currentReadingStatusId
                    );
        }

        if (exists) {
            throw new BadRequestException(
                    "A reading status already exists for this content"
            );
        }
    }

    private UserEntity getAuthenticatedUserWithSubscription() {
        UserEntity principalUser = getAuthenticatedUser();

        return userRepository.findByIdWithSubscription(principalUser.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Authenticated user not found")
                );
    }

    private void validatePercentageConsistency(
            ReadingStatusRequestDto requestDto
    ) {
        if (requestDto.status() == ReadingStatusEnum.FINISHED
                && requestDto.progressValue() != 100) {
            throw new BadRequestException(
                    "FINISHED with percentage progress requires 100 percent"
            );
        }

        if (requestDto.status() != ReadingStatusEnum.FINISHED
                && requestDto.progressValue() == 100) {
            throw new BadRequestException(
                    "A progress of 100 percent requires FINISHED status"
            );
        }
    }

    private void validateChapterConsistency(
            ReadingStatusRequestDto requestDto
    ) {
        if (requestDto.storyId() != null) {
            validateStoryChapterProgress(requestDto);
        }
    }

    private void validateStoryChapterProgress(
            ReadingStatusRequestDto requestDto
    ) {
        chapterRepository
                .findByStoryIdExternalAndChapterNumberAndDeletedFalseAndPublishedTrue(
                        requestDto.storyId(),
                        requestDto.progressValue()
                )
                .orElseThrow(() ->
                        new BadRequestException(
                                "Published chapter "
                                        + requestDto.progressValue()
                                        + " does not exist for this story"
                        )
                );

        if (requestDto.status() != ReadingStatusEnum.FINISHED) {
            return;
        }

        int lastPublishedChapter =
                chapterRepository
                        .findTopByStoryIdExternalAndDeletedFalseAndPublishedTrueOrderByChapterNumberDesc(
                                requestDto.storyId()
                        )
                        .map(chapter -> chapter.getChapterNumber())
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "The story has no published chapters"
                                )
                        );

        if (!requestDto.progressValue().equals(lastPublishedChapter)) {
            throw new BadRequestException(
                    "A finished story must be recorded at its last published chapter"
            );
        }
    }
}
