package com.lms.service;

import com.lms.entity.Course;
import com.lms.entity.ExamApprovalRequest;
import com.lms.entity.User;
import com.lms.repository.CourseRepository;
import com.lms.repository.ExamApprovalRequestRepository;
import com.lms.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExamApprovalRequestService {

    private final ExamApprovalRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final ExamEligibilityService eligibilityService;

    public ExamApprovalRequestService(
            ExamApprovalRequestRepository requestRepository,
            UserRepository userRepository,
            CourseRepository courseRepository,
            ExamEligibilityService eligibilityService
    ) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.eligibilityService = eligibilityService;
    }

    // STUDENT REQUEST
    public ExamApprovalRequest requestApproval(
            Long studentId,
            Long courseId
    ) {

        // First check Materials 100% + Coding 100%
        var eligibility =
                eligibilityService.checkEligibility(
                        studentId,
                        courseId
                );

        if (!eligibility.isEligibleForExamApproval()) {
            throw new RuntimeException(
                    "Complete 100% Study Materials and 100% Coding Challenges first"
            );
        }

        // Check existing request
        var existing =
                requestRepository
                        .findByStudent_IdAndCourse_Id(
                                studentId,
                                courseId
                        );

        if (existing.isPresent()) {

            ExamApprovalRequest request =
                    existing.get();

            if (request.getStatus()
                    == ExamApprovalRequest.Status.PENDING) {

                throw new RuntimeException(
                        "Exam approval request is already pending"
                );
            }

            if (request.getStatus()
                    == ExamApprovalRequest.Status.APPROVED) {

                throw new RuntimeException(
                        "Exam is already approved"
                );
            }
        }

        User student =
                userRepository
                        .findById(studentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Student not found"
                                ));

        Course course =
                courseRepository
                        .findById(courseId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Course not found"
                                ));

        ExamApprovalRequest request =
                new ExamApprovalRequest();

        request.setStudent(student);
        request.setCourse(course);
        request.setStatus(
                ExamApprovalRequest.Status.PENDING
        );

        return requestRepository.save(request);
    }

    // FACULTY VIEW REQUESTS
   @Transactional(readOnly = true)
public List<ExamApprovalRequest> getPendingRequests(
        Authentication authentication) {

    User faculty = getAuthenticatedUser(authentication);

    if (!hasRole(faculty, "FACULTY")) {
        throw new AccessDeniedException(
                "Only faculty members can view pending requests"
        );
    }

    return requestRepository.findByCourse_Faculty_IdAndStatus(
            faculty.getId(),
            ExamApprovalRequest.Status.PENDING
    );
}

    // FACULTY APPROVE
    public ExamApprovalRequest approve(
        Long requestId,
        String comment,
        Authentication authentication) {

    User faculty = getAuthenticatedUser(authentication);

    ExamApprovalRequest request = requestRepository.findById(requestId)
            .orElseThrow(() ->
                    new IllegalArgumentException(
                            "Approval request not found"
                    ));

    verifyFacultyCourseAccess(faculty, request.getCourse());

    if (request.getStatus() != ExamApprovalRequest.Status.PENDING) {
        throw new IllegalStateException(
                "Only pending requests can be approved"
        );
    }

    request.setStatus(ExamApprovalRequest.Status.APPROVED);
    request.setFacultyComment(comment);
    request.setReviewedAt(java.time.LocalDateTime.now());

    return requestRepository.save(request);
}

    // FACULTY REJECT
public ExamApprovalRequest reject(
        Long requestId,
        String comment,
        Authentication authentication) {

    User faculty = getAuthenticatedUser(authentication);

    ExamApprovalRequest request = requestRepository.findById(requestId)
            .orElseThrow(() ->
                    new IllegalArgumentException(
                            "Approval request not found"
                    ));

    verifyFacultyCourseAccess(faculty, request.getCourse());

    if (request.getStatus() != ExamApprovalRequest.Status.PENDING) {
        throw new IllegalStateException(
                "Only pending requests can be rejected"
        );
    }

    request.setStatus(ExamApprovalRequest.Status.REJECTED);
    request.setFacultyComment(comment);
    request.setReviewedAt(java.time.LocalDateTime.now());

    return requestRepository.save(request);
}
    // STUDENT - CHECK APPROVAL STATUS
public String getApprovalStatus(
        Long studentId,
        Long courseId
) {

    var request =
            requestRepository
                    .findByStudent_IdAndCourse_Id(
                            studentId,
                            courseId
                    );

    if (request.isEmpty()) {
        return "NOT_REQUESTED";
    }

    return request.get().getStatus().name();
}
private User getAuthenticatedUser(Authentication authentication) {

    if (authentication == null
            || !authentication.isAuthenticated()
            || !(authentication.getPrincipal() instanceof UserDetails)) {
        throw new AccessDeniedException("Authentication is required");
    }

    UserDetails userDetails =
            (UserDetails) authentication.getPrincipal();

    return userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() ->
                    new IllegalStateException(
                            "Authenticated user was not found"
                    ));
}

private boolean hasRole(User user, String roleName) {
    return user.getRole() != null
            && user.getRole().getName() != null
            && user.getRole().getName().equalsIgnoreCase(roleName);
}

private void verifyFacultyCourseAccess(User faculty, Course course) {

    if (!hasRole(faculty, "FACULTY")) {
        throw new AccessDeniedException(
                "Only faculty members can review exam approval requests"
        );
    }

    if (course.getFaculty() == null
            || !course.getFaculty().getId().equals(faculty.getId())) {
        throw new AccessDeniedException(
                "You are not assigned to this course"
        );
    }
}
}