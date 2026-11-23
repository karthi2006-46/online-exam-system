package com.lms.controller;

import com.lms.dto.admin.AdminSettingsDTO;
import com.lms.service.AdminSettingsService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Platform settings edited from the admin console.
 */
@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettingsController {

    private final AdminSettingsService settingsService;

    @GetMapping
    public AdminSettingsDTO get() {
        return settingsService.get();
    }

    @PutMapping
    public AdminSettingsDTO update(@RequestBody AdminSettingsDTO request) {
        return settingsService.update(request);
    }
}
