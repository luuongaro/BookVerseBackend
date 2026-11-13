package com.grupo3.BookVerse.features.groups.groupGoals.controller;

import com.grupo3.BookVerse.features.groups.groupGoals.dto.GroupGoalsResponseDto;
import com.grupo3.BookVerse.features.groups.groupGoals.service.GroupGoalsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reading-groups/{groupId}/goals/history")
public class GroupGoalHistoryController {

    private final GroupGoalsService groupGoalsService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<GroupGoalsResponseDto>> getGoalsHistory(
            @PathVariable UUID groupId
    ) {
        return ResponseEntity.ok(
                groupGoalsService.getGoalsByGroupId(groupId)
        );
    }
}
