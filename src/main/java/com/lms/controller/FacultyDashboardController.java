package com.lms.controller;

import com.lms.dto.FacultyDashboardDTO;
import com.lms.service.FacultyDashboardService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/faculty/dashboard")
public class FacultyDashboardController {

    private final FacultyDashboardService dashboardService;

    public FacultyDashboardController(
            FacultyDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @PreAuthorize("hasRole('FACULTY')")
    public FacultyDashboardDTO getDashboard(Authentication authentication) {
        return dashboardService.getDashboard(authentication);
    }
}