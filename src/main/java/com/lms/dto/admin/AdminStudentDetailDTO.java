package com.lms.dto.admin;

import com.lms.dto.StudentProgressReportDTO;
import com.lms.dto.UserDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin view of one student: profile plus one full report per enrollment.
 * The report DTO is the same one used by the faculty PDF report.
 */
@Data
@NoArgsConstructor
public class AdminStudentDetailDTO {

    private UserDTO profile;

    private long enrolledCourses;
    private long completedCourses;

    private long totalResults;
    private long passedResults;
    private long failedResults;
    private double averagePercentage;

    private List<StudentProgressReportDTO> courseReports = new ArrayList<>();
}
