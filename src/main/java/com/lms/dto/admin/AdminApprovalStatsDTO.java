package com.lms.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Headline counters for the admin Exam Approvals page.
 */
@Data
@NoArgsConstructor
public class AdminApprovalStatsDTO {

    private long pending;
    private long approved;
    private long rejected;
    private long total;
}
