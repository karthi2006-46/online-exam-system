package com.lms.service;

import com.lms.entity.Course;
import com.lms.entity.ExamApprovalRequest;
import com.lms.entity.User;
import com.lms.repository.CourseRepository;
import com.lms.repository.ExamApprovalRequestRepository;
import com.lms.repository.UserRepository;
import org.springframework.stereotype.Service;

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
    public List<ExamApprovalRequest> getPendingRequests() {

        return requestRepository.findByStatus(
                ExamApprovalRequest.Status.PENDING
        );
    }

    // FACULTY APPROVE
    public ExamApprovalRequest approve(
            Long requestId,
            String comment
    ) {

        ExamApprovalRequest request =
                requestRepository
                        .findById(requestId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Approval request not found"
                                ));

        request.setStatus(
                ExamApprovalRequest.Status.APPROVED
        );

        request.setFacultyComment(comment);
        request.setReviewedAt(
                java.time.LocalDateTime.now()
        );

        return requestRepository.save(request);
    }

    // FACULTY REJECT
    public ExamApprovalRequest reject(
            Long requestId,
            String comment
    ) {

        ExamApprovalRequest request =
                requestRepository
                        .findById(requestId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Approval request not found"
                                ));

        request.setStatus(
                ExamApprovalRequest.Status.REJECTED
        );

        request.setFacultyComment(comment);
        request.setReviewedAt(
                java.time.LocalDateTime.now()
        );

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
}