package com.lms.service;

import com.lms.dto.FacultyDashboardDTO;
import com.lms.entity.Result;
import com.lms.repository.CourseRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.ExamApprovalRequestRepository;
import com.lms.repository.ExamRepository;
import com.lms.repository.ExtensionRequestRepository;
import com.lms.repository.ResultRepository;
import com.lms.repository.StudyMaterialRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class FacultyDashboardService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudyMaterialRepository materialRepository;
    private final ExamRepository examRepository;
    private final ExamApprovalRequestRepository approvalRepository;
    private final ExtensionRequestRepository extensionRepository;
    private final ResultRepository resultRepository;
    private final com.lms.repository.UserRepository userRepository;

    public FacultyDashboardService(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            StudyMaterialRepository materialRepository,
            ExamRepository examRepository,
            ExamApprovalRequestRepository approvalRepository,
            ExtensionRequestRepository extensionRepository,
            ResultRepository resultRepository,
            com.lms.repository.UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.materialRepository = materialRepository;
        this.examRepository = examRepository;
        this.approvalRepository = approvalRepository;
        this.extensionRepository = extensionRepository;
        this.resultRepository = resultRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public FacultyDashboardDTO getDashboard(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserDetails)) {
            throw new AccessDeniedException("Authenticated faculty required");
        }

        UserDetails details = (UserDetails) authentication.getPrincipal();

        var faculty = userRepository.findByEmail(details.getUsername())
                .orElseThrow(() -> new AccessDeniedException("Faculty not found"));

        if (faculty.getRole() == null
                || faculty.getRole().getName() == null
                || !faculty.getRole().getName().equalsIgnoreCase("FACULTY")) {
            throw new AccessDeniedException("Faculty access required");
        }

        Long facultyId = faculty.getId();

        FacultyDashboardDTO dto = new FacultyDashboardDTO();

        dto.setTotalCourses(courseRepository.findByFaculty_Id(facultyId).size());

        long totalEnrollments =
                enrollmentRepository.countByCourse_Faculty_Id(facultyId);

        long activeEnrollments =
                enrollmentRepository.countByCourse_Faculty_IdAndStatusIgnoreCase(
                        facultyId, "ACTIVE");

        dto.setTotalStudents(totalEnrollments);
        dto.setActiveStudents(activeEnrollments);
        dto.setInactiveStudents(Math.max(0, totalEnrollments - activeEnrollments));

        dto.setTotalMaterials(
                materialRepository.countByCourse_Faculty_IdAndActiveTrue(facultyId));

        dto.setTotalExams(
                examRepository.countByCourse_Faculty_IdAndActiveTrue(facultyId));

        dto.setPendingExamApprovals(
                approvalRepository.findByCourse_Faculty_IdAndStatus(
                        facultyId,
                        com.lms.entity.ExamApprovalRequest.Status.PENDING
                ).size());

        dto.setPendingExtensionRequests(
                extensionRepository
                        .countByEnrollment_Course_Faculty_IdAndStatusIgnoreCase(
                                facultyId, "PENDING"));

        List<Result> results = resultRepository
                .findByExam_Course_Faculty_Id(facultyId);

        dto.setTotalResults(results.size());

        long passed = results.stream()
                .filter(r -> r.getStatus() != null
                        && r.getStatus().equalsIgnoreCase("PASS"))
                .count();

        long failed = results.stream()
                .filter(r -> r.getStatus() != null
                        && r.getStatus().equalsIgnoreCase("FAIL"))
                .count();

        dto.setPassedResults(passed);
        dto.setFailedResults(failed);

        double average = results.stream()
                .filter(r -> r.getPercentage() != null)
                .mapToDouble(Result::getPercentage)
                .average()
                .orElse(0.0);

        dto.setAverageScore(Math.round(average * 100.0) / 100.0);

        double passPercentage = results.isEmpty()
                ? 0.0
                : (passed * 100.0) / results.size();

        dto.setPassPercentage(Math.round(passPercentage * 100.0) / 100.0);

        return dto;
    }
}