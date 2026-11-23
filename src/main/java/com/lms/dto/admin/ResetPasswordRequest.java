package com.lms.dto.admin;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin payload for resetting a faculty password.
 */
@Data
@NoArgsConstructor
public class ResetPasswordRequest {

    @Size(min = 8, max = 100, message = "Password must contain at least 8 characters")
    private String newPassword;

    private boolean generatePassword = true;
}
