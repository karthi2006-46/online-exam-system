package com.lms.controller;

import com.lms.dto.admin.AdminReportOverviewDTO;
import com.lms.service.AdminReportService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Platform-wide reporting for the admin console.
 */
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final AdminReportService reportService;

    @GetMapping("/overview")
    public AdminReportOverviewDTO overview() {
        return reportService.overview();
    }
}
