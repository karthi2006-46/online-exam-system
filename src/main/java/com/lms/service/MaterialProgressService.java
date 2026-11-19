package com.lms.service;

import com.lms.dto.MaterialViewProgressRequest;
import com.lms.entity.Enrollment;
import com.lms.entity.MaterialProgress;
import com.lms.entity.StudyMaterial;
import com.lms.entity.User;

import com.lms.repository.EnrollmentRepository;
import com.lms.repository.MaterialProgressRepository;
import com.lms.repository.StudyMaterialRepository;
import com.lms.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialProgressService {

    private final MaterialProgressRepository progressRepo;
    private final StudyMaterialRepository materialRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final UserRepository userRepo;

    // ======================================
    // GET OR CREATE PROGRESS
    // ======================================

    private MaterialProgress getOrCreateProgress(
            Long studentId,
            Long materialId) {

        User student = userRepo.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student not found"
                ));

        if (student.getRole() == null ||
                !"STUDENT".equalsIgnoreCase(
                        student.getRole().getName())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only students can update progress"
            );
        }

        if (Boolean.FALSE.equals(student.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Inactive students cannot update progress"
            );
        }

        StudyMaterial material = materialRepo.findById(materialId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Study material not found"
                ));

        Long courseId = material.getCourse().getId();

        enrollmentRepo.findByStudent_IdAndCourse_Id(
                studentId,
                courseId
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You are not enrolled in this course"
        ));

        return progressRepo
                .findByStudent_IdAndMaterial_Id(
                        studentId,
                        materialId
                )
                .orElseGet(() -> {
                    MaterialProgress progress =
                            new MaterialProgress();

                    progress.setStudent(student);
                    progress.setMaterial(material);
                    progress.setCompleted(false);

                    return progress;
                });
    }

    // ======================================
    // RECORD PDF PROGRESS
    // ======================================

    @Transactional
    public MaterialProgress recordPdfProgress(
            Long studentId,
            Long materialId,
            MaterialViewProgressRequest request) {

        MaterialProgress progress =
                getOrCreateProgress(studentId, materialId);

        int page = request.getCurrentPage() == null
                ? 0 : request.getCurrentPage();

        int total = request.getTotalPages() == null
                ? 0 : request.getTotalPages();

        if (page < 1 || total < 1 || page > total ||
                total > 10000) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid PDF page information"
            );
        }

        if (progress.getTotalPages() > 0 &&
                !progress.getTotalPages().equals(total)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "PDF page count changed"
            );
        }

        progress.setTotalPages(total);

        // Store the highest page reached.
        progress.setLastPage(
                Math.max(progress.getLastPage(), page)
        );

        return progressRepo.save(progress);
    }

    // ======================================
    // RECORD VIDEO PROGRESS
    // ======================================

    @Transactional
    public MaterialProgress recordVideoProgress(
            Long studentId,
            Long materialId,
            MaterialViewProgressRequest request) {

        MaterialProgress progress =
                getOrCreateProgress(studentId, materialId);

        double currentTime = request.getCurrentTime() == null
                ? 0 : request.getCurrentTime();

        double duration = request.getDuration() == null
                ? 0 : request.getDuration();

        boolean playing = Boolean.TRUE.equals(
                request.getPlaying()
        );

        if (!Double.isFinite(currentTime) ||
                !Double.isFinite(duration) ||
                duration <= 0 ||
                duration > 86400 ||
                currentTime < 0 ||
                currentTime > duration) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid video progress information"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        if (progress.getLastHeartbeatAt() != null &&
                playing &&
                request.getPlaying() != null) {

            long elapsed = Duration.between(
                    progress.getLastHeartbeatAt(),
                    now
            ).getSeconds();

            double previousPosition =
                    progress.getLastPlaybackPosition();

            double positionDifference =
                    currentTime - previousPosition;

            // Count only plausible forward playback.
            if (elapsed > 0 &&
                    elapsed <= 15 &&
                    positionDifference > 0 &&
                    positionDifference <= elapsed + 3) {

                progress.setWatchedSeconds(
                        progress.getWatchedSeconds() + elapsed
                );
            }
        }

        progress.setVideoDuration(duration);
        progress.setLastPlaybackPosition(currentTime);
        progress.setLastHeartbeatAt(now);

        return progressRepo.save(progress);
    }

    // ======================================
    // RECORD TEXT COMPLETION
    // ======================================

    @Transactional
    public MaterialProgress recordTextProgress(
            Long studentId,
            Long materialId,
            MaterialViewProgressRequest request) {

        MaterialProgress progress =
                getOrCreateProgress(studentId, materialId);

        if (!Boolean.TRUE.equals(request.getReachedBottom())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You must reach the bottom of the material"
            );
        }

        progress.setTextCompleted(true);

        return progressRepo.save(progress);
    }

    // ======================================
    // MARK MATERIAL COMPLETED
    // ======================================

    @Transactional
    public MaterialProgress markCompleted(
            Long studentId,
            Long materialId) {

        MaterialProgress progress =
                getOrCreateProgress(studentId, materialId);

        StudyMaterial material = progress.getMaterial();

        String type = material.getFileType() == null
                ? ""
                : material.getFileType().toUpperCase();

        boolean eligible;

        if (type.contains("PDF")) {

            eligible =
                    progress.getTotalPages() > 0 &&
                    progress.getLastPage() >=
                            progress.getTotalPages();

        } else if (
                type.contains("VIDEO") ||
                type.contains("MP4")) {

            double duration = progress.getVideoDuration();

            eligible =
                    duration > 0 &&
                    progress.getWatchedSeconds() >=
                            Math.max(0, duration - 60);

        } else if (
                type.contains("TEXT") ||
                type.contains("NOTES")) {

            eligible = Boolean.TRUE.equals(
                    progress.getTextCompleted()
            );

        } else {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Automatic completion tracking is not supported for this material type"
            );
        }

        if (!eligible) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Please complete the required viewing before marking this material completed"
            );
        }

        if (!Boolean.TRUE.equals(progress.getCompleted())) {

            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());

            progressRepo.save(progress);

            Long studentIdValue = progress.getStudent().getId();
            Long courseId = material.getCourse().getId();

            Enrollment enrollment = enrollmentRepo
                    .findByStudent_IdAndCourse_Id(
                            studentIdValue,
                            courseId
                    )
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Enrollment not found"
                    ));

            updateEnrollmentProgress(
                    studentIdValue,
                    courseId,
                    enrollment
            );
        }

        return progress;
    }

    // ======================================
    // GET COMPLETED MATERIAL IDS
    // ======================================

    @Transactional(readOnly = true)
    public List<Long> getCompletedMaterialIds(Long studentId) {

        return progressRepo.findByStudent_Id(studentId)
                .stream()
                .filter(progress ->
                        Boolean.TRUE.equals(progress.getCompleted()))
                .map(progress ->
                        progress.getMaterial().getId())
                .toList();
    }

    // ======================================
    // UPDATE COURSE PROGRESS
    // ======================================

    private void updateEnrollmentProgress(
            Long studentId,
            Long courseId,
            Enrollment enrollment) {

        long totalMaterials =
                materialRepo.countByCourse_Id(courseId);

        if (totalMaterials == 0) {
            enrollment.setCompletionPercentage(0.0);
            enrollmentRepo.save(enrollment);
            return;
        }

        long completedMaterials =
                progressRepo
                        .countByStudent_IdAndMaterial_Course_IdAndCompleted(
                                studentId,
                                courseId,
                                true
                        );

        double percentage =
                completedMaterials * 100.0 / totalMaterials;

        enrollment.setCompletionPercentage(percentage);

        enrollmentRepo.save(enrollment);
    }
}
