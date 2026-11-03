package com.grupo3.BookVerse.features.storyReport.services.impl;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.DuplicateResourceException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.stories.domain.StoryEntity;
import com.grupo3.BookVerse.features.stories.repository.StoryRepository;
import com.grupo3.BookVerse.features.storyReport.domain.StoryReportEntity;
import com.grupo3.BookVerse.features.storyReport.domain.StoryReportStatus;
import com.grupo3.BookVerse.features.storyReport.dtos.StoryReportCreateRequestDto;
import com.grupo3.BookVerse.features.storyReport.dtos.StoryReportModerationRequestDto;
import com.grupo3.BookVerse.features.storyReport.dtos.StoryReportResponseDto;
import com.grupo3.BookVerse.features.storyReport.mapper.StoryReportMapper;
import com.grupo3.BookVerse.features.storyReport.repository.StoryReportRepository;
import com.grupo3.BookVerse.features.storyReport.services.StoryReportService;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoryReportServiceImpl implements StoryReportService {

    private final StoryReportRepository storyReportRepository;
    private final StoryReportMapper storyReportMapper;
    private final StoryRepository storyRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StoryReportResponseDto> getAllReports() {
        return storyReportMapper.toResponseListDto(
                storyReportRepository.findAllByOrderByCreatedAtDesc()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoryReportResponseDto> getPendingReports() {
        return storyReportMapper.toResponseListDto(
                storyReportRepository.findByStatusOrderByCreatedAtDesc(
                        StoryReportStatus.PENDING
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public StoryReportResponseDto getReportById(UUID reportId) {
        return storyReportMapper.toResponseDto(findReportByIdExternal(reportId));
    }

    @Override
    @Transactional
    public StoryReportResponseDto createReport(StoryReportCreateRequestDto dto) {
        UserEntity reporter = getAuthenticatedUser();
        StoryEntity story = findActiveStoryByIdExternal(dto.storyId());

        if (story.getAuthor().getId().equals(reporter.getId())) {
            throw new BadRequestException("Users cannot report their own stories");
        }

        boolean alreadyReported =
                storyReportRepository.existsByStory_IdAndReporterUser_IdAndStatus(
                        story.getId(),
                        reporter.getId(),
                        StoryReportStatus.PENDING
                );

        if (alreadyReported) {
            throw new DuplicateResourceException(
                    "There is already a pending report for this story by the same user"
            );
        }

        StoryReportEntity report = storyReportMapper.toEntity(dto);
        report.setStory(story);
        report.setReporterUser(reporter);
        report.setStatus(StoryReportStatus.PENDING);

        return storyReportMapper.toResponseDto(
                storyReportRepository.save(report)
        );
    }

    @Override
    @Transactional
    public StoryReportResponseDto approveReport(
            UUID reportId,
            StoryReportModerationRequestDto dto
    ) {
        StoryReportEntity report = findPendingReportByIdExternal(reportId);
        UserEntity moderator = getAuthenticatedUser();

        StoryEntity story = report.getStory();
        story.setDeleted(true);
        storyRepository.save(story);

        resolvePendingReportsForStory(
                story.getId(),
                moderator,
                StoryReportStatus.APPROVED,
                dto.resolutionComment()
        );

        return storyReportMapper.toResponseDto(report);
    }

    @Override
    @Transactional
    public StoryReportResponseDto rejectReport(
            UUID reportId,
            StoryReportModerationRequestDto dto
    ) {
        StoryReportEntity report = findPendingReportByIdExternal(reportId);

        resolvePendingReportsForStory(
                report.getStory().getId(),
                getAuthenticatedUser(),
                StoryReportStatus.REJECTED,
                dto.resolutionComment()
        );

        return storyReportMapper.toResponseDto(report);
    }

    private void resolvePendingReportsForStory(
            Long storyId,
            UserEntity moderator,
            StoryReportStatus resolutionStatus,
            String resolutionComment
    ) {
        List<StoryReportEntity> pendingReports =
                storyReportRepository.findAllByStory_IdAndStatus(
                        storyId,
                        StoryReportStatus.PENDING
                );

        LocalDateTime resolvedAt = LocalDateTime.now();

        for (StoryReportEntity pendingReport : pendingReports) {
            pendingReport.setModeratorUser(moderator);
            pendingReport.setStatus(resolutionStatus);
            pendingReport.setResolutionComment(resolutionComment);
            pendingReport.setResolvedAt(resolvedAt);
        }

        storyReportRepository.saveAll(pendingReports);
    }

    private StoryReportEntity findReportByIdExternal(UUID reportId) {
        return storyReportRepository.findByIdExternal(reportId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Story report not found with idExternal: " + reportId
                ));
    }

    private StoryReportEntity findPendingReportByIdExternal(UUID reportId) {
        StoryReportEntity report = findReportByIdExternal(reportId);

        if (report.getStatus() != StoryReportStatus.PENDING) {
            throw new BadRequestException("Only pending reports can be moderated");
        }

        return report;
    }

    private StoryEntity findActiveStoryByIdExternal(UUID storyId) {
        return storyRepository.findByIdExternalAndDeletedFalse(storyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Story not found with idExternal: " + storyId
                ));
    }

    private UserEntity getAuthenticatedUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal() instanceof UserEntity user)) {
            throw new AccessDeniedException("Authenticated user not found");
        }

        return user;
    }
}
