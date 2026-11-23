package com.lms.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Platform settings shown on the admin Settings page.
 */
@Data
@NoArgsConstructor
public class AdminSettingsDTO {

    private String platformName;
    private String supportEmail;
    private String supportPhone;
    private Boolean allowStudentSignup;
    private Integer defaultCourseDurationDays;
    private Double defaultCourseFees;
    private LocalDateTime updatedAt;
}
