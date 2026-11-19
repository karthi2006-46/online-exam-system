package com.lms.service;

import com.lms.entity.Enrollment;
import com.lms.repository.EnrollmentRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CourseExpiryScheduler {

    private final EnrollmentRepository enrollmentRepository;

    // Check expired enrollments every minute
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void updateExpiredEnrollments() {

        LocalDateTime now = LocalDateTime.now();

        List<Enrollment> enrollments =
                enrollmentRepository.findAll();

        for (Enrollment enrollment : enrollments) {

            if (enrollment.getDueDate() == null) {
                continue;
            }

            String status = enrollment.getStatus();

            // Do not modify completed or dropped enrollments
            if ("COMPLETED".equalsIgnoreCase(status)
                    || "DROPPED".equalsIgnoreCase(status)) {
                continue;
            }

            if (!enrollment.getDueDate().isAfter(now)) {

                if (!"INACTIVE".equalsIgnoreCase(status)) {
                    enrollment.setStatus("INACTIVE");
                }

            } else {

                if ("INACTIVE".equalsIgnoreCase(status)) {
                    enrollment.setStatus("ACTIVE");
                }
            }
        }

        enrollmentRepository.saveAll(enrollments);
    }
}