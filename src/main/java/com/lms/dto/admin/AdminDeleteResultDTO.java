package com.lms.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of an admin delete request.
 * mode is DELETED (no history existed) or DEACTIVATED (history preserved).
 */
@Data
@NoArgsConstructor
public class AdminDeleteResultDTO {

    private Long id;
    private String mode;
    private String message;

    public AdminDeleteResultDTO(Long id, String mode, String message) {
        this.id = id;
        this.mode = mode;
        this.message = message;
    }
}
