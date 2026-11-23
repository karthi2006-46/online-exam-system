package com.lms.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of an admin password reset.
 */
@Data
@NoArgsConstructor
public class AdminPasswordResetDTO {

    private Long userId;
    private boolean passwordGenerated;
    private String temporaryPassword;
}
