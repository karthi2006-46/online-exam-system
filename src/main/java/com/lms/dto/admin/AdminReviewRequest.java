package com.lms.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Reviewer comment sent when an admin approves or rejects a request.
 */
@Data
@NoArgsConstructor
public class AdminReviewRequest {

    private String comment;
}
