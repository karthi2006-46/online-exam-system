package com.lms.dto.admin;

import com.lms.dto.FacultyDashboardDTO;
import com.lms.dto.UserDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Detailed faculty view opened from the admin Faculty Management table.
 */
@Data
@NoArgsConstructor
public class AdminFacultyDetailDTO {

    private UserDTO profile;
    private FacultyDashboardDTO dashboard;

    private List<AdminCourseSummaryDTO> courses = new ArrayList<>();

    private long pendingCodingSubmissions;
    private long approvedCodingSubmissions;
    private long rejectedCodingSubmissions;

    private long pendingExamApprovals;
    private long approvedExamRequests;
    private long rejectedExamRequests;

    private long pendingExtensionRequests;
    private long approvedExtensionRequests;
    private long rejectedExtensionRequests;
}
