package com.lms.controller;

import com.lms.dto.admin.AdminPageDTO;
import com.lms.dto.admin.AdminPerformanceRowDTO;
import com.lms.entity.Exam;
import com.lms.repository.ExamRepository;
import com.lms.service.AdminPerformanceService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/performance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPerformanceController {

    private final AdminPerformanceService performanceService;
    private final ExamRepository examRepository;

    /**
     * Student performance matrix: one row per enrollment.
     */
    @GetMapping
    public AdminPageDTO<AdminPerformanceRowDTO> performance(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long facultyId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String approvalStatus,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return performanceService.performance(
                courseId,
                facultyId,
                status,
                approvalStatus,
                search,
                page,
                size
        );
    }

    /**
     * Exam options for the results page filter.
     */
    @GetMapping("/exams")
    public List<Map<String, Object>> exams(
            @RequestParam Long courseId) {

        performanceService.requireCourse(courseId);

        return examRepository.findByCourse_Id(courseId)
                .stream()
                .sorted((a, b) -> {
                    String left = a.getTitle() == null ? "" : a.getTitle();
                    String right = b.getTitle() == null ? "" : b.getTitle();
                    return left.compareToIgnoreCase(right);
                })
                .map(this::toExamOption)
                .toList();
    }

    private Map<String, Object> toExamOption(Exam exam) {

        Map<String, Object> option = new LinkedHashMap<>();
        option.put("id", exam.getId());
        option.put("title", exam.getTitle());
        option.put("totalMarks", exam.getTotalMarks());
        option.put("published", exam.getPublished());

        return option;
    }
}
