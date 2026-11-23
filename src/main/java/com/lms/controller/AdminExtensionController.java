package com.lms.controller;

import com.lms.dto.admin.AdminExtensionRejectRequest;
import com.lms.dto.admin.AdminExtensionRowDTO;
import com.lms.dto.admin.AdminExtensionStatsDTO;
import com.lms.dto.admin.AdminPageDTO;
import com.lms.service.AdminExtensionService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin review of every course extension request.
 */
@RestController
@RequestMapping("/api/admin/extensions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminExtensionController {

    private final AdminExtensionService extensionService;

    @GetMapping
    public AdminPageDTO<AdminExtensionRowDTO> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long facultyId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return extensionService.list(
                status, courseId, facultyId, search, page, size);
    }

    @GetMapping("/stats")
    public AdminExtensionStatsDTO stats() {
        return extensionService.stats();
    }

    @PostMapping("/{requestId}/approve")
    public AdminExtensionRowDTO approve(
            @PathVariable Long requestId,
            Authentication authentication) {

        return extensionService.approve(requestId, authentication);
    }

    @PostMapping("/{requestId}/reject")
    public AdminExtensionRowDTO reject(
            @PathVariable Long requestId,
            @RequestBody AdminExtensionRejectRequest request,
            Authentication authentication) {

        return extensionService.reject(
                requestId,
                request == null ? null : request.getReason(),
                authentication
        );
    }
}
