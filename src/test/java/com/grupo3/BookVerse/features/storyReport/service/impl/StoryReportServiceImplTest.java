package com.grupo3.BookVerse.features.storyReport.service.impl;

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
import com.grupo3.BookVerse.features.storyReport.services.impl.StoryReportServiceImpl;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoryReportServiceImplTest {

    @Mock
    private StoryReportRepository storyReportRepository;

    @Mock
    private StoryReportMapper storyReportMapper;

    @Mock
    private StoryRepository storyRepository;

    @InjectMocks
    private StoryReportServiceImpl storyReportService;

    private UUID reportId;
    private UUID storyId;
    private UUID reporterUserId;

    @BeforeEach
    void setUp() {
        reportId = UUID.randomUUID();
        storyId = UUID.randomUUID();
        reporterUserId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllReports_shouldReturnList() {
        List<StoryReportEntity> reports = List.of(new StoryReportEntity());
        List<StoryReportResponseDto> response = List.of(mock(StoryReportResponseDto.class));

        when(storyReportRepository.findAllByOrderByCreatedAtDesc()).thenReturn(reports);
        when(storyReportMapper.toResponseListDto(reports)).thenReturn(response);

        List<StoryReportResponseDto> result = storyReportService.getAllReports();

        assertEquals(response, result);
        verify(storyReportRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void getPendingReports_shouldReturnList() {
        List<StoryReportEntity> reports = List.of(new StoryReportEntity());
        List<StoryReportResponseDto> response = List.of(mock(StoryReportResponseDto.class));

        when(storyReportRepository.findByStatusOrderByCreatedAtDesc(StoryReportStatus.PENDING))
                .thenReturn(reports);
        when(storyReportMapper.toResponseListDto(reports)).thenReturn(response);

        List<StoryReportResponseDto> result = storyReportService.getPendingReports();

        assertEquals(response, result);
    }

    @Test
    void getReportById_shouldReturnDto() {
        StoryReportEntity entity = new StoryReportEntity();
        StoryReportResponseDto response = mock(StoryReportResponseDto.class);

        when(storyReportRepository.findByIdExternal(reportId)).thenReturn(Optional.of(entity));
        when(storyReportMapper.toResponseDto(entity)).thenReturn(response);

        StoryReportResponseDto result = storyReportService.getReportById(reportId);

        assertEquals(response, result);
    }

    @Test
    void getReportById_shouldThrowException_whenNotFound() {
        when(storyReportRepository.findByIdExternal(reportId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> storyReportService.getReportById(reportId)
        );
    }

    @Test
    void createReport_shouldReturnDto() {
        UserEntity reporter = new UserEntity();
        reporter.setId(1L);
        reporter.setIdExternal(reporterUserId);

        UserEntity author = new UserEntity();
        author.setId(2L);

        StoryEntity story = new StoryEntity();
        story.setId(10L);
        story.setAuthor(author);
        story.setDeleted(false);

        StoryReportCreateRequestDto dto = new StoryReportCreateRequestDto(
                storyId,
                "Spam content",
                "This story violates the rules"
        );

        StoryReportEntity entity = new StoryReportEntity();
        StoryReportEntity saved = new StoryReportEntity();
        StoryReportResponseDto response = mock(StoryReportResponseDto.class);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(reporter, null)
        );

        when(storyRepository.findByIdExternalAndDeletedFalse(storyId)).thenReturn(Optional.of(story));
        when(storyReportRepository.existsByStory_IdAndReporterUser_IdAndStatus(
                10L,
                1L,
                StoryReportStatus.PENDING
        )).thenReturn(false);
        when(storyReportMapper.toEntity(dto)).thenReturn(entity);
        when(storyReportRepository.save(entity)).thenReturn(saved);
        when(storyReportMapper.toResponseDto(saved)).thenReturn(response);

        StoryReportResponseDto result = storyReportService.createReport(dto);

        assertEquals(response, result);
        verify(storyReportRepository).save(entity);
    }

    @Test
    void createReport_shouldThrow_whenDuplicatePendingReport() {
        UserEntity reporter = new UserEntity();
        reporter.setId(1L);
        reporter.setIdExternal(reporterUserId);

        UserEntity author = new UserEntity();
        author.setId(2L);

        StoryEntity story = new StoryEntity();
        story.setId(10L);
        story.setAuthor(author);
        story.setDeleted(false);

        StoryReportCreateRequestDto dto = new StoryReportCreateRequestDto(
                storyId,
                "Spam content",
                "This story violates the rules"
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(reporter, null)
        );

        when(storyRepository.findByIdExternalAndDeletedFalse(storyId)).thenReturn(Optional.of(story));
        when(storyReportRepository.existsByStory_IdAndReporterUser_IdAndStatus(
                10L,
                1L,
                StoryReportStatus.PENDING
        )).thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> storyReportService.createReport(dto)
        );
    }

    @Test
    void createReport_shouldThrow_whenUserReportsOwnStory() {
        UserEntity reporter = new UserEntity();
        reporter.setId(1L);
        reporter.setIdExternal(reporterUserId);

        StoryEntity story = new StoryEntity();
        story.setId(10L);
        story.setAuthor(reporter);
        story.setDeleted(false);

        StoryReportCreateRequestDto dto = new StoryReportCreateRequestDto(
                storyId,
                "Spam content",
                "This story violates the rules"
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(reporter, null)
        );

        when(storyRepository.findByIdExternalAndDeletedFalse(storyId)).thenReturn(Optional.of(story));

        assertThrows(
                BadRequestException.class,
                () -> storyReportService.createReport(dto)
        );
    }

    @Test
    void approveReport_shouldApproveSuccessfully() {
        UserEntity moderator = authenticateModerator();

        StoryEntity story = new StoryEntity();
        story.setId(10L);
        story.setDeleted(false);

        StoryReportEntity report = new StoryReportEntity();
        report.setStatus(StoryReportStatus.PENDING);
        report.setStory(story);

        StoryReportModerationRequestDto dto = new StoryReportModerationRequestDto(
                "Inappropriate content"
        );

        StoryReportResponseDto response = mock(StoryReportResponseDto.class);

        when(storyReportRepository.findByIdExternal(reportId)).thenReturn(Optional.of(report));
        when(storyReportRepository.findAllByStory_IdAndStatus(10L, StoryReportStatus.PENDING))
                .thenReturn(List.of(report));
        when(storyReportMapper.toResponseDto(report)).thenReturn(response);
        when(storyRepository.save(story)).thenReturn(story);

        StoryReportResponseDto result = storyReportService.approveReport(reportId, dto);

        assertEquals(response, result);
        assertEquals(StoryReportStatus.APPROVED, report.getStatus());
        assertTrue(story.isDeleted());
        assertTrue(story.isModerationRemoved());
        assertSame(moderator, report.getModeratorUser());
        verify(storyRepository).save(story);
    }

    @Test
    void approveReport_shouldThrow_whenStoryWasAlreadyDeleted() {
        authenticateModerator();

        StoryEntity story = new StoryEntity();
        story.setDeleted(true);

        StoryReportEntity report = new StoryReportEntity();
        report.setStatus(StoryReportStatus.PENDING);
        report.setStory(story);

        StoryReportModerationRequestDto dto = new StoryReportModerationRequestDto(
                "Inappropriate content"
        );

        when(storyReportRepository.findByIdExternal(reportId)).thenReturn(Optional.of(report));

        BadRequestException error = assertThrows(
                BadRequestException.class,
                () -> storyReportService.approveReport(reportId, dto)
        );

        assertEquals("No se puede aprobar el reporte: la historia ya fue eliminada", error.getMessage());
        assertFalse(story.isModerationRemoved());
        assertEquals(StoryReportStatus.PENDING, report.getStatus());
        verifyNoInteractions(storyRepository);
        verify(storyReportRepository, never()).saveAll(any());
    }

    @Test
    void rejectReport_shouldRejectSuccessfully() {
        UserEntity moderator = authenticateModerator();

        StoryEntity story = new StoryEntity();
        story.setId(10L);

        StoryReportEntity report = new StoryReportEntity();
        report.setStatus(StoryReportStatus.PENDING);
        report.setStory(story);

        StoryReportModerationRequestDto dto = new StoryReportModerationRequestDto(
                "Not enough evidence"
        );

        StoryReportResponseDto response = mock(StoryReportResponseDto.class);

        when(storyReportRepository.findByIdExternal(reportId)).thenReturn(Optional.of(report));
        when(storyReportRepository.findAllByStory_IdAndStatus(10L, StoryReportStatus.PENDING))
                .thenReturn(List.of(report));
        when(storyReportMapper.toResponseDto(report)).thenReturn(response);

        StoryReportResponseDto result = storyReportService.rejectReport(reportId, dto);

        assertEquals(response, result);
        assertEquals(StoryReportStatus.REJECTED, report.getStatus());
        assertSame(moderator, report.getModeratorUser());
    }

    @Test
    void restoreStory_shouldClearRemovalFlagsAndRecordRestorationOnAllApprovedReports() {
        UserEntity restoredBy = authenticateModerator();
        StoryEntity story = StoryEntity.builder()
                .id(10L)
                .deleted(true)
                .moderationRemoved(true)
                .build();
        StoryReportEntity report = new StoryReportEntity();
        report.setStatus(StoryReportStatus.APPROVED);
        report.setStory(story);
        StoryReportEntity otherReport = new StoryReportEntity();
        otherReport.setStatus(StoryReportStatus.APPROVED);
        otherReport.setStory(story);
        List<StoryReportEntity> approvedReports = List.of(report, otherReport);
        when(storyReportRepository.findByIdExternal(reportId)).thenReturn(Optional.of(report));
        when(storyReportRepository.findAllByStory_IdAndStatus(story.getId(), StoryReportStatus.APPROVED))
                .thenReturn(approvedReports);

        storyReportService.restoreStory(reportId);

        assertFalse(story.isDeleted());
        assertFalse(story.isModerationRemoved());
        assertNotNull(report.getRestoredAt());
        for (StoryReportEntity approvedReport : approvedReports) {
            assertSame(restoredBy, approvedReport.getRestoredByUser());
            assertEquals(report.getRestoredAt(), approvedReport.getRestoredAt());
            assertEquals(StoryReportStatus.APPROVED, approvedReport.getStatus());
        }
        verify(storyRepository).save(story);
        verify(storyReportRepository).saveAll(approvedReports);
    }

    @ParameterizedTest
    @EnumSource(value = StoryReportStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "APPROVED")
    void restoreStory_shouldRejectReportsThatAreNotApproved(StoryReportStatus status) {
        StoryReportEntity report = new StoryReportEntity();
        report.setStatus(status);
        when(storyReportRepository.findByIdExternal(reportId)).thenReturn(Optional.of(report));

        assertThrows(BadRequestException.class, () -> storyReportService.restoreStory(reportId));

        verifyNoInteractions(storyRepository);
    }

    @Test
    void restoreStory_shouldFailWhenTheAssociatedStoryIsMissing() {
        StoryReportEntity report = new StoryReportEntity();
        report.setStatus(StoryReportStatus.APPROVED);
        when(storyReportRepository.findByIdExternal(reportId)).thenReturn(Optional.of(report));

        assertThrows(ResourceNotFoundException.class, () -> storyReportService.restoreStory(reportId));

        verifyNoInteractions(storyRepository);
    }

    @ParameterizedTest
    @CsvSource({"true, false", "false, true", "false, false"})
    void restoreStory_shouldRejectStoriesThatAreNotCurrentlyRemovedByModeration(
            boolean deleted, boolean moderationRemoved
    ) {
        StoryEntity story = StoryEntity.builder()
                .deleted(deleted)
                .moderationRemoved(moderationRemoved)
                .build();
        StoryReportEntity report = new StoryReportEntity();
        report.setStatus(StoryReportStatus.APPROVED);
        report.setStory(story);
        when(storyReportRepository.findByIdExternal(reportId)).thenReturn(Optional.of(report));

        assertThrows(BadRequestException.class, () -> storyReportService.restoreStory(reportId));

        assertEquals(deleted, story.isDeleted());
        assertEquals(moderationRemoved, story.isModerationRemoved());
        verifyNoInteractions(storyRepository);
    }

    private UserEntity authenticateModerator() {
        UserEntity moderator = new UserEntity();
        moderator.setId(99L);
        moderator.setIdExternal(UUID.randomUUID());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(moderator, null)
        );

        return moderator;
    }
}
