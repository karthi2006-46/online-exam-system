package com.lms.dto.admin;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Reason sent when an admin rejects an extension request.
 */
@Data
@NoArgsConstructor
public class AdminExtensionRejectRequest {

    private String reason;
}
