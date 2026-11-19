package com.lms.service;

import com.lms.entity.Course;
import com.lms.entity.ExamApprovalRequest;
import com.lms.entity.User;
import com.lms.repository.CourseRepository;
import com.lms.repository.ExamApprovalRequestRepository;
import com.lms.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
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

    // ==========================================
    // STUDENT - REQUEST EXAM APPROVAL
    // ==========================================

    public ExamApprovalRequest requestApproval(
            Authentication authentication,
            Long courseId
    ) {

        User student = getAuthenticatedUser(authentication);

        if (!hasRole(student, "STUDENT")) {
            throw new AccessDeniedException(
                    "Only students can request exam approval"
            );
        }

        Long studentId = student.getId();

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Course not found")
                );

        // Check 100% study materials and 100% approved coding challenges
        var eligibility = eligibilityService.checkEligibility(
                studentId,
                courseId
        );

        if (!eligibility.isEligibleForExamApproval()) {
            throw new IllegalStateException(
                    "Complete 100% Study Materials and get 100% Coding Challenges approved first"
            );
        }

        // Check existing request
        var existing = requestRepository
                .findByStudent_IdAndCourse_Id(studentId, courseId);

        if (existing.isPresent()) {

            ExamApprovalRequest request = existing.get();

            if (request.getStatus() == ExamApprovalRequest.Status.PENDING) {
                throw new IllegalStateException(
                        "Exam approval request is already pending"
                );
            }

            if (request.getStatus() == ExamApprovalRequest.Status.APPROVED) {
                throw new IllegalStateException(
                        "Exam is already approved"
                );
            }
        }

        ExamApprovalRequest request = new ExamApprovalRequest();

        request.setStudent(student);
        request.setCourse(course);
        request.setStatus(ExamApprovalRequest.Status.PENDING);

        return requestRepository.save(request);
    }

    // ==========================================
    // FACULTY - VIEW PENDING REQUESTS
    // ==========================================

    @Transactional(readOnly = true)
    public List<ExamApprovalRequest> getPendingRequests(
            Authentication authentication
    ) {

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

    // ==========================================
    // FACULTY - APPROVE EXAM REQUEST
    // ==========================================

    public ExamApprovalRequest approve(
            Long requestId,
            String comment,
            Authentication authentication
    ) {

        User faculty = getAuthenticatedUser(authentication);

        ExamApprovalRequest request = requestRepository.findById(requestId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Approval request not found"
                        ));

        // Verify faculty is assigned to this course
        verifyFacultyCourseAccess(faculty, request.getCourse());

        // Only pending requests can be approved
        if (request.getStatus() != ExamApprovalRequest.Status.PENDING) {
            throw new IllegalStateException(
                    "Only pending requests can be approved"
            );
        }

        // ==========================================
        // RECHECK STUDENT ELIGIBILITY
        // ==========================================

        var eligibility = eligibilityService.checkEligibility(
                request.getStudent().getId(),
                request.getCourse().getId()
        );

        if (!eligibility.isEligibleForExamApproval()) {
            throw new IllegalStateException(
                    "Student is no longer eligible for exam approval"
            );
        }

        // ==========================================
        // APPROVE EXAM REQUEST
        // ==========================================

        request.setStatus(ExamApprovalRequest.Status.APPROVED);
        request.setFacultyComment(comment);
        request.setReviewedAt(java.time.LocalDateTime.now());

        return requestRepository.save(request);
    }

    // ==========================================
    // FACULTY - REJECT EXAM REQUEST
    // ==========================================

    public ExamApprovalRequest reject(
            Long requestId,
            String comment,
            Authentication authentication
    ) {

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

    // ==========================================
    // STUDENT - CHECK EXAM APPROVAL STATUS
    // ==========================================

    public String getApprovalStatus(
            Authentication authentication,
            Long courseId
    ) {

        User student = getAuthenticatedUser(authentication);

        if (!hasRole(student, "STUDENT")) {
            throw new AccessDeniedException(
                    "Only students can check exam approval status"
            );
        }

        var request = requestRepository
                .findByStudent_IdAndCourse_Id(
                        student.getId(),
                        courseId
                );

        if (request.isEmpty()) {
            return "NOT_REQUESTED";
        }

        return request.get().getStatus().name();
    }

    // ==========================================
    // GET AUTHENTICATED USER
    // ==========================================

    private User getAuthenticatedUser(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserDetails)) {

            throw new AccessDeniedException(
                    "Authentication is required"
            );
        }

        UserDetails userDetails =
                (UserDetails) authentication.getPrincipal();

        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user was not found"
                        ));
    }

    // ==========================================
    // ROLE VALIDATION
    // ==========================================

    private boolean hasRole(User user, String roleName) {

        return user.getRole() != null
                && user.getRole().getName() != null
                && user.getRole().getName().equalsIgnoreCase(roleName);
    }

    // ==========================================
    // FACULTY COURSE ACCESS VALIDATION
    // ==========================================

    private void verifyFacultyCourseAccess(
            User faculty,
            Course course
    ) {

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