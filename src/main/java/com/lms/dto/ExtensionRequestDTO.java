package com.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtensionRequestDTO {
    private Long id;
    private Long enrollmentId;
    private Integer daysRequested;
    private String status;
    private String reason;
    private String rejectionReason;
}
