package com.grupo3.BookVerse.features.groups.groupCommentReport.service.impl;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.groups.groupComment.domain.GroupCommentEntity;
import com.grupo3.BookVerse.features.groups.groupComment.domain.GroupCommentStatus;
import com.grupo3.BookVerse.features.groups.groupComment.repository.GroupCommentRepository;
import com.grupo3.BookVerse.features.groups.groupCommentReport.domain.GroupCommentReportEntity;
import com.grupo3.BookVerse.features.groups.groupCommentReport.domain.GroupCommentReportStatus;
import com.grupo3.BookVerse.features.groups.groupCommentReport.repository.GroupCommentReportRepository;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupCommentReportServiceImplTest {

    @Mock
    private GroupCommentReportRepository reportRepository;

    @Mock
    private GroupCommentRepository commentRepository;

    @InjectMocks
    private GroupCommentReportServiceImpl reportService;

    private final UUID reportId = UUID.randomUUID();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void restoreComment_shouldActivateCommentAndRecordRestorationOnAllApprovedReports() {
        UserEntity restoredBy = new UserEntity();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(restoredBy, null)
        );
        GroupCommentEntity comment = new GroupCommentEntity();
        comment.setId(10L);
        comment.setStatus(GroupCommentStatus.MODERATED);
        GroupCommentReportEntity report = stubReport(GroupCommentReportStatus.APPROVED, comment);
        GroupCommentReportEntity otherReport = new GroupCommentReportEntity();
        otherReport.setStatus(GroupCommentReportStatus.APPROVED);
        otherReport.setComment(comment);
        List<GroupCommentReportEntity> approvedReports = List.of(report, otherReport);
        when(reportRepository.findAllByCommentIdAndStatus(comment.getId(), GroupCommentReportStatus.APPROVED))
                .thenReturn(approvedReports);

        reportService.restoreComment(reportId);

        assertEquals(GroupCommentStatus.ACTIVE, comment.getStatus());
        assertNotNull(report.getRestoredAt());
        for (GroupCommentReportEntity approvedReport : approvedReports) {
            assertSame(restoredBy, approvedReport.getRestoredByUser());
            assertEquals(report.getRestoredAt(), approvedReport.getRestoredAt());
            assertEquals(GroupCommentReportStatus.APPROVED, approvedReport.getStatus());
        }
        verify(commentRepository).save(comment);
        verify(reportRepository).saveAll(approvedReports);
    }

    @ParameterizedTest
    @EnumSource(value = GroupCommentReportStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "APPROVED")
    void restoreComment_shouldRejectReportsThatAreNotApproved(GroupCommentReportStatus status) {
        GroupCommentEntity comment = new GroupCommentEntity();
        comment.setStatus(GroupCommentStatus.MODERATED);
        stubReport(status, comment);

        assertThrows(BadRequestException.class, () -> reportService.restoreComment(reportId));

        assertEquals(GroupCommentStatus.MODERATED, comment.getStatus());
        verifyNoInteractions(commentRepository);
    }

    @ParameterizedTest
    @EnumSource(value = GroupCommentStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "MODERATED")
    void restoreComment_shouldRejectCommentsThatAreNotModerated(GroupCommentStatus status) {
        GroupCommentEntity comment = new GroupCommentEntity();
        comment.setStatus(status);
        stubReport(GroupCommentReportStatus.APPROVED, comment);

        assertThrows(BadRequestException.class, () -> reportService.restoreComment(reportId));

        assertEquals(status, comment.getStatus());
        verifyNoInteractions(commentRepository);
    }

    @Test
    void restoreComment_shouldFailWhenTheAssociatedCommentIsMissing() {
        stubReport(GroupCommentReportStatus.APPROVED, null);

        assertThrows(ResourceNotFoundException.class, () -> reportService.restoreComment(reportId));

        verifyNoInteractions(commentRepository);
    }

    private GroupCommentReportEntity stubReport(GroupCommentReportStatus status, GroupCommentEntity comment) {
        GroupCommentReportEntity report = new GroupCommentReportEntity();
        report.setStatus(status);
        report.setComment(comment);
        when(reportRepository.findByIdExternal(reportId)).thenReturn(Optional.of(report));
        return report;
    }
}
