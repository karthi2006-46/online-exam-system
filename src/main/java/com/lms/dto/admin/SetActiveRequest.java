package com.lms.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Activate / deactivate payload for faculty and student accounts.
 */
@Data
@NoArgsConstructor
public class SetActiveRequest {

    @NotNull(message = "active is required")
    private Boolean active;
}
