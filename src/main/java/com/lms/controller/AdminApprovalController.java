package com.lms.controller;

import com.lms.dto.admin.AdminApprovalRowDTO;
import com.lms.dto.admin.AdminApprovalStatsDTO;
import com.lms.dto.admin.AdminPageDTO;
import com.lms.dto.admin.AdminReviewRequest;
import com.lms.service.AdminApprovalService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Admin review of every exam approval request.
 */
@RestController
@RequestMapping("/api/admin/approvals")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminApprovalController {

    private final AdminApprovalService approvalService;

    @GetMapping
    public AdminPageDTO<AdminApprovalRowDTO> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long facultyId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return approvalService.list(
                status, courseId, facultyId, search, page, size);
    }

    @GetMapping("/stats")
    public AdminApprovalStatsDTO stats() {
        return approvalService.stats();
    }

    @GetMapping("/options/courses")
    public List<Map<String, Object>> courseOptions() {
        return approvalService.courseOptions();
    }

    @GetMapping("/options/faculty")
    public List<Map<String, Object>> facultyOptions() {
        return approvalService.facultyOptions();
    }

    @PostMapping("/{requestId}/approve")
    public AdminApprovalRowDTO approve(
            @PathVariable Long requestId,
            @RequestBody(required = false) AdminReviewRequest request) {

        return approvalService.approve(
                requestId,
                request == null ? null : request.getComment());
    }

    @PostMapping("/{requestId}/reject")
    public AdminApprovalRowDTO reject(
            @PathVariable Long requestId,
            @RequestBody(required = false) AdminReviewRequest request) {

        return approvalService.reject(
                requestId,
                request == null ? null : request.getComment());
    }
}
