package com.lms.controller;

import com.lms.dto.admin.AdminDeleteResultDTO;
import com.lms.dto.admin.AdminFacultyAccountDTO;
import com.lms.dto.admin.AdminFacultyDetailDTO;
import com.lms.dto.admin.AdminFacultyRowDTO;
import com.lms.dto.admin.AdminPageDTO;
import com.lms.dto.admin.AdminPasswordResetDTO;
import com.lms.dto.admin.AssignCoursesRequest;
import com.lms.dto.admin.CreateFacultyRequest;
import com.lms.dto.admin.ResetPasswordRequest;
import com.lms.dto.admin.SetActiveRequest;
import com.lms.dto.admin.UpdateFacultyRequest;
import com.lms.service.AdminFacultyService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/faculty")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFacultyController {

    private final AdminFacultyService facultyService;

    @GetMapping
    public AdminPageDTO<AdminFacultyRowDTO> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return facultyService.list(search, active, page, size);
    }

    @GetMapping("/{facultyId}")
    public AdminFacultyDetailDTO detail(@PathVariable Long facultyId) {
        return facultyService.detail(facultyId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminFacultyAccountDTO create(
            @Valid @RequestBody CreateFacultyRequest request) {

        return facultyService.create(request);
    }

    @PutMapping("/{facultyId}")
    public AdminFacultyRowDTO update(
            @PathVariable Long facultyId,
            @Valid @RequestBody UpdateFacultyRequest request) {

        return facultyService.update(facultyId, request);
    }

    @PatchMapping("/{facultyId}/status")
    public AdminFacultyRowDTO setActive(
            @PathVariable Long facultyId,
            @Valid @RequestBody SetActiveRequest request) {

        return facultyService.setActive(facultyId, request.getActive());
    }

    @PostMapping("/{facultyId}/courses")
    public AdminFacultyDetailDTO assignCourses(
            @PathVariable Long facultyId,
            @RequestBody AssignCoursesRequest request) {

        return facultyService.assignCourses(facultyId, request);
    }

    @PostMapping("/{facultyId}/reset-password")
    public AdminPasswordResetDTO resetPassword(
            @PathVariable Long facultyId,
            @RequestBody ResetPasswordRequest request) {

        return facultyService.resetPassword(facultyId, request);
    }

    @DeleteMapping("/{facultyId}")
    public AdminDeleteResultDTO delete(@PathVariable Long facultyId) {
        return facultyService.delete(facultyId);
    }
}
