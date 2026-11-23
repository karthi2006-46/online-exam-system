package com.lms.service;

import com.lms.dto.admin.AdminSettingsDTO;
import com.lms.entity.SystemSetting;
import com.lms.exception.BadRequestException;
import com.lms.repository.SystemSettingRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;

/**
 * Persists the small set of platform settings edited from the admin console.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AdminSettingsService {

    public static final String PLATFORM_NAME = "platformName";
    public static final String SUPPORT_EMAIL = "supportEmail";
    public static final String SUPPORT_PHONE = "supportPhone";
    public static final String ALLOW_STUDENT_SIGNUP = "allowStudentSignup";
    public static final String DEFAULT_COURSE_DURATION = "defaultCourseDurationDays";
    public static final String DEFAULT_COURSE_FEES = "defaultCourseFees";

    private static final String DEFAULT_PLATFORM_NAME = "LearnHub LMS";
    private static final int DEFAULT_DURATION_DAYS = 30;
    private static final double DEFAULT_FEES = 0.0;

    private final SystemSettingRepository settingRepository;

    @Transactional(readOnly = true)
    public AdminSettingsDTO get() {

        AdminSettingsDTO dto = new AdminSettingsDTO();

        dto.setPlatformName(
                read(PLATFORM_NAME).orElse(DEFAULT_PLATFORM_NAME));
        dto.setSupportEmail(read(SUPPORT_EMAIL).orElse(""));
        dto.setSupportPhone(read(SUPPORT_PHONE).orElse(""));
        dto.setAllowStudentSignup(
                read(ALLOW_STUDENT_SIGNUP)
                        .map(value -> !"false".equalsIgnoreCase(value))
                        .orElse(Boolean.TRUE));
        dto.setDefaultCourseDurationDays(
                read(DEFAULT_COURSE_DURATION)
                        .map(this::parseInt)
                        .orElse(DEFAULT_DURATION_DAYS));
        dto.setDefaultCourseFees(
                read(DEFAULT_COURSE_FEES)
                        .map(this::parseDouble)
                        .orElse(DEFAULT_FEES));

        settingRepository.findAll().stream()
                .map(SystemSetting::getUpdatedAt)
                .filter(value -> value != null)
                .max(Comparator.naturalOrder())
                .ifPresent(dto::setUpdatedAt);

        return dto;
    }

    public AdminSettingsDTO update(AdminSettingsDTO request) {

        if (request == null) {
            throw new BadRequestException("Settings payload is required");
        }

        String platformName = trim(request.getPlatformName());

        if (platformName == null || platformName.isEmpty()) {
            throw new BadRequestException("Platform name is required");
        }

        String supportEmail = trim(request.getSupportEmail());

        if (supportEmail != null
                && !supportEmail.isEmpty()
                && !supportEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new BadRequestException("Enter a valid support email address");
        }

        int duration = request.getDefaultCourseDurationDays() == null
                ? DEFAULT_DURATION_DAYS
                : request.getDefaultCourseDurationDays();

        if (duration < 1 || duration > 3650) {
            throw new BadRequestException(
                    "Default course duration must be between 1 and 3650 days");
        }

        double fees = request.getDefaultCourseFees() == null
                ? DEFAULT_FEES
                : request.getDefaultCourseFees();

        if (fees < 0) {
            throw new BadRequestException("Default course fees cannot be negative");
        }

        boolean allowSignup = request.getAllowStudentSignup() == null
                || request.getAllowStudentSignup();

        save(PLATFORM_NAME, platformName);
        save(SUPPORT_EMAIL, supportEmail == null ? "" : supportEmail);
        save(SUPPORT_PHONE, trim(request.getSupportPhone()) == null
                ? ""
                : trim(request.getSupportPhone()));
        save(ALLOW_STUDENT_SIGNUP, String.valueOf(allowSignup));
        save(DEFAULT_COURSE_DURATION, String.valueOf(duration));
        save(DEFAULT_COURSE_FEES, String.valueOf(fees));

        return get();
    }

    /* ---------------- used by other services ---------------- */

    @Transactional(readOnly = true)
    public boolean isStudentSignupAllowed() {
        return read(ALLOW_STUDENT_SIGNUP)
                .map(value -> !"false".equalsIgnoreCase(value))
                .orElse(Boolean.TRUE);
    }

    @Transactional(readOnly = true)
    public int defaultCourseDurationDays() {
        return read(DEFAULT_COURSE_DURATION)
                .map(this::parseInt)
                .orElse(DEFAULT_DURATION_DAYS);
    }

    @Transactional(readOnly = true)
    public double defaultCourseFees() {
        return read(DEFAULT_COURSE_FEES)
                .map(this::parseDouble)
                .orElse(DEFAULT_FEES);
    }

    /* ---------------- helpers ---------------- */

    private Optional<String> read(String key) {
        return settingRepository.findById(key)
                .map(SystemSetting::getValue)
                .filter(value -> value != null && !value.isBlank());
    }

    private void save(String key, String value) {

        SystemSetting setting = settingRepository.findById(key)
                .orElseGet(() -> {
                    SystemSetting created = new SystemSetting();
                    created.setKey(key);
                    return created;
                });

        setting.setValue(value);
        setting.setUpdatedAt(LocalDateTime.now());

        settingRepository.save(setting);
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException error) {
            return DEFAULT_DURATION_DAYS;
        }
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException error) {
            return DEFAULT_FEES;
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
