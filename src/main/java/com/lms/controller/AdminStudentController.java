package com.lms.controller;

import com.lms.dto.admin.AdminDeleteResultDTO;
import com.lms.dto.admin.AdminPageDTO;
import com.lms.dto.admin.AdminResultRowDTO;
import com.lms.dto.admin.AdminStudentDetailDTO;
import com.lms.dto.admin.AdminStudentRowDTO;
import com.lms.dto.admin.SetActiveRequest;
import com.lms.service.AdminPerformanceService;
import com.lms.service.AdminStudentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStudentController {

    private final AdminStudentService studentService;
    private final AdminPerformanceService performanceService;

    @GetMapping
    public AdminPageDTO<AdminStudentRowDTO> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return studentService.list(search, active, courseId, page, size);
    }

    @GetMapping("/{studentId}")
    public AdminStudentDetailDTO detail(@PathVariable Long studentId) {
        return studentService.detail(studentId);
    }

    @GetMapping("/{studentId}/results")
    public List<AdminResultRowDTO> results(@PathVariable Long studentId) {
        return performanceService.studentResults(studentId);
    }

    @PatchMapping("/{studentId}/status")
    public AdminStudentRowDTO setActive(
            @PathVariable Long studentId,
            @Valid @RequestBody SetActiveRequest request) {

        return studentService.setActive(studentId, request.getActive());
    }

    @DeleteMapping("/{studentId}")
    public AdminDeleteResultDTO delete(@PathVariable Long studentId) {
        return studentService.delete(studentId);
    }
}
