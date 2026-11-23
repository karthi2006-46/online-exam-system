package com.lms.controller;

import com.lms.dto.admin.AdminCourseRequest;
import com.lms.dto.admin.AdminCourseRowDTO;
import com.lms.dto.admin.AdminDeleteResultDTO;
import com.lms.dto.admin.AdminPageDTO;
import com.lms.dto.admin.SetActiveRequest;
import com.lms.service.AdminCourseService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Admin course management.
 */
@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCourseController {

    private final AdminCourseService courseService;

    @GetMapping
    public AdminPageDTO<AdminCourseRowDTO> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long facultyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return courseService.list(search, active, facultyId, page, size);
    }

    @GetMapping("/options")
    public List<Map<String, Object>> options() {
        return courseService.courseOptions();
    }

    @GetMapping("/{courseId}")
    public AdminCourseRowDTO detail(@PathVariable Long courseId) {
        return courseService.detail(courseId);
    }

    @PostMapping
    public AdminCourseRowDTO create(
            @Valid @RequestBody AdminCourseRequest request) {

        return courseService.create(request);
    }

    @PutMapping("/{courseId}")
    public AdminCourseRowDTO update(
            @PathVariable Long courseId,
            @Valid @RequestBody AdminCourseRequest request) {

        return courseService.update(courseId, request);
    }

    @PatchMapping("/{courseId}/active")
    public AdminCourseRowDTO setActive(
            @PathVariable Long courseId,
            @Valid @RequestBody SetActiveRequest request) {

        return courseService.setActive(courseId, request.getActive());
    }

    @DeleteMapping("/{courseId}")
    public AdminDeleteResultDTO delete(@PathVariable Long courseId) {
        return courseService.delete(courseId);
    }
}
