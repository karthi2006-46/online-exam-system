package com.lms.controller;

import com.lms.dto.admin.AdminPageDTO;
import com.lms.dto.admin.AdminResultRowDTO;
import com.lms.service.AdminPerformanceService;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/results")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminResultController {

    private final AdminPerformanceService performanceService;

    /**
     * Paged results for a course, optionally filtered by exam, date and status.
     */
    @GetMapping
    public AdminPageDTO<AdminResultRowDTO> results(
            @RequestParam Long courseId,
            @RequestParam(required = false) Long examId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return performanceService.results(
                courseId,
                examId,
                fromDate == null ? null : fromDate.atStartOfDay(),
                toDate == null ? null : toDate.atTime(23, 59, 59),
                status,
                page,
                size
        );
    }

    /**
     * All results for one student across courses.
     */
    @GetMapping("/student/{studentId}")
    public List<AdminResultRowDTO> studentResults(
            @PathVariable Long studentId) {

        return performanceService.studentResults(studentId);
    }
}
