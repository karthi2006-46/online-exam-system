package com.lms.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Headline counters for the admin Extension Requests page.
 */
@Data
@NoArgsConstructor
public class AdminExtensionStatsDTO {

    private long pending;
    private long approved;
    private long rejected;
    private long total;
}
