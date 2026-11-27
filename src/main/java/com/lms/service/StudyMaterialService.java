        package com.lms.service;

import com.lms.dto.StudyMaterialDTO;
import com.lms.entity.Course;
import com.lms.entity.Enrollment;
import com.lms.entity.StudyMaterial;
import com.lms.entity.User;
import com.lms.exception.BadRequestException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.CourseRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.StudyMaterialRepository;
import com.lms.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudyMaterialService {

    private final StudyMaterialRepository materialRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    public StudyMaterialService(
            StudyMaterialRepository materialRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            EnrollmentRepository enrollmentRepository) {

        this.materialRepository = materialRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    private static final Path UPLOAD_DIR =
            Paths.get("uploads", "materials")
                    .toAbsolutePath()
                    .normalize();

    private static final Set<String> ALLOWED_FILE_TYPES =
            Set.of("PDF", "VIDEO", "ASSIGNMENT");

    private static final long MAX_FILE_SIZE =
            50L * 1024 * 1024;

    // =========================================================
    // AUTHENTICATION HELPERS
    // =========================================================

    private User getAuthenticatedUser(
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null
                || !(authentication.getPrincipal()
                        instanceof UserDetails)) {

            throw new AccessDeniedException(
                    "Authenticated user required");
        }

        UserDetails details =
                (UserDetails) authentication.getPrincipal();

        return userRepository.findByEmail(details.getUsername())
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Authenticated user not found"));
    }

    private String getRole(User user) {

        if (user.getRole() == null
                || user.getRole().getName() == null) {
            return "";
        }

        return user.getRole().getName();
    }

    private boolean isAdmin(User user) {
        return "ADMIN".equalsIgnoreCase(getRole(user));
    }

    private boolean isFaculty(User user) {
        return "FACULTY".equalsIgnoreCase(getRole(user));
    }

    private boolean isStudent(User user) {
        return "STUDENT".equalsIgnoreCase(getRole(user));
    }

    private void requireActiveUser(User user) {

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new AccessDeniedException(
                    "Your account is inactive");
        }
    }

    private void requireFacultyOrAdmin(User user) {

        requireActiveUser(user);

        if (!isFaculty(user) && !isAdmin(user)) {
            throw new AccessDeniedException(
                    "Faculty or admin access required");
        }
    }

    // =========================================================
    // COURSE AUTHORIZATION
    // =========================================================

    private Course getAuthorizedCourse(
            Long courseId,
            User user) {

        requireActiveUser(user);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Course not found"));

        if (!Boolean.TRUE.equals(course.getActive())) {
            throw new BadRequestException(
                    "This course is inactive");
        }

        // ADMIN can access active courses.
        if (isAdmin(user)) {
            return course;
        }

        // FACULTY can access only their assigned courses.
        if (isFaculty(user)) {

            if (course.getFaculty() == null
                    || !course.getFaculty().getId()
                            .equals(user.getId())) {

                throw new AccessDeniedException(
                        "You are not assigned to this course");
            }

            return course;
        }

// STUDENT must have an active enrollment and an unexpired due date.
if (isStudent(user)) {

    Enrollment enrollment = enrollmentRepository
            .findByStudent_IdAndCourse_Id(
                    user.getId(),
                    courseId)
            .orElseThrow(() ->
                    new AccessDeniedException(
                            "You are not enrolled in this course"));

    if (enrollment.getDueDate() != null
            && !enrollment.getDueDate()
                    .isAfter(LocalDateTime.now())) {

        throw new AccessDeniedException(
                "You can't access the study materials because the course due date has passed.");
    }

    if (!"ACTIVE".equalsIgnoreCase(enrollment.getStatus())) {
        throw new AccessDeniedException(
                "Your course due date has expired.");
    }

    return course;
}

        throw new AccessDeniedException(
                "You do not have permission to access this course");
    }

    // =========================================================
    // MATERIAL AUTHORIZATION
    // =========================================================

    private StudyMaterial getActiveMaterial(Long materialId) {

        return materialRepository.findById(materialId)
                .filter(material ->
                        Boolean.TRUE.equals(material.getActive()))
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Active study material not found"));
    }

    private StudyMaterial getAuthorizedMaterial(
            Long materialId,
            User user) {

        StudyMaterial material = getActiveMaterial(materialId);

        if (material.getCourse() == null) {
            throw new ResourceNotFoundException(
                    "Material course not found");
        }

        getAuthorizedCourse(
                material.getCourse().getId(),
                user);

        return material;
    }

    // =========================================================
    // UPLOAD MATERIAL
    // =========================================================

    public StudyMaterialDTO uploadMaterial(
            Authentication authentication,
            Long courseId,
            String title,
            String description,
            String fileType,
            MultipartFile file) throws IOException {

        User user = getAuthenticatedUser(authentication);

        requireFacultyOrAdmin(user);

        Course course = getAuthorizedCourse(courseId, user);

        if (title == null || title.isBlank()) {
            throw new BadRequestException(
                    "Material title is required");
        }

        if (fileType == null || fileType.isBlank()) {
            throw new BadRequestException(
                    "Material type is required");
        }

        String normalizedType =
                fileType.trim().toUpperCase();

        if (!ALLOWED_FILE_TYPES.contains(normalizedType)) {
            throw new BadRequestException(
                    "Invalid material type. Allowed types: "
                            + "PDF, VIDEO, ASSIGNMENT");
        }

        if (file == null || file.isEmpty()) {
            throw new BadRequestException(
                    "Please select a file to upload");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException(
                    "File size must not exceed 50 MB");
        }

        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null
                || originalFileName.isBlank()) {

            throw new BadRequestException(
                    "Invalid filename");
        }

        // Prevent client-supplied directory paths.
        String safeOriginalName = Paths.get(originalFileName)
                .getFileName()
                .toString();

        if (safeOriginalName.isBlank()
                || safeOriginalName.contains("..")) {

            throw new BadRequestException(
                    "Invalid filename");
        }

        // Validate the file extension against the selected type.
        String lowerName = safeOriginalName.toLowerCase();

        boolean validExtension;

        switch (normalizedType) {

            case "PDF":
                validExtension = lowerName.endsWith(".pdf");
                break;

            case "VIDEO":
                validExtension = lowerName.matches(
                        ".*\\.(mp4|webm|mov|m4v)");
                break;

            case "ASSIGNMENT":
                validExtension = lowerName.matches(
                        ".*\\.(pdf|doc|docx|txt|zip)");
                break;

            default:
                validExtension = false;
        }

        if (!validExtension) {
            throw new BadRequestException(
                    "File extension does not match "
                            + "the selected material type");
        }

        Files.createDirectories(UPLOAD_DIR);

        String storedFileName =
                UUID.randomUUID() + "_" + safeOriginalName;

        Path filePath = UPLOAD_DIR.resolve(storedFileName)
                .normalize();

        if (!filePath.startsWith(UPLOAD_DIR)) {
            throw new BadRequestException(
                    "Invalid upload path");
        }

        // Save the uploaded file.
        file.transferTo(filePath);

        try {
            StudyMaterial material = new StudyMaterial();

            material.setCourse(course);
            material.setTitle(title.trim());

            material.setDescription(
                    description == null
                            ? ""
                            : description.trim());

            material.setFileType(normalizedType);

            /*
             * This URL must be served by a matching download
             * endpoint or resource handler.
             */
            material.setFileUrl(
                    "/api/materials/" + storedFileName
                            + "/download");

            material.setFileName(safeOriginalName);
            material.setFileSize(file.getSize());
            material.setActive(true);

            StudyMaterial savedMaterial =
                    materialRepository.save(material);

            return convertToDTO(savedMaterial);

        } catch (RuntimeException ex) {

            // Remove the saved file if database saving fails.
            Files.deleteIfExists(filePath);

            throw ex;
        }
    }

    // =========================================================
    // GET MATERIAL BY ID
    // =========================================================

    @Transactional(readOnly = true)
    public StudyMaterialDTO getMaterialById(
            Authentication authentication,
            Long materialId) {

        User user = getAuthenticatedUser(authentication);

        StudyMaterial material =
                getAuthorizedMaterial(materialId, user);

        return convertToDTO(material);
    }

    // =========================================================
    // GET ALL ACTIVE MATERIALS FOR A COURSE
    // =========================================================

    @Transactional(readOnly = true)
    public List<StudyMaterialDTO> getCourseMaterials(
            Authentication authentication,
            Long courseId) {

        User user = getAuthenticatedUser(authentication);

        // Validates the role and access to this course.
        getAuthorizedCourse(courseId, user);

        return materialRepository
                .findByCourse_IdAndActiveTrue(courseId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // =========================================================
    // GET ACTIVE MATERIALS BY COURSE AND TYPE
    // =========================================================

    @Transactional(readOnly = true)
    public List<StudyMaterialDTO> getCourseMaterialsByType(
            Authentication authentication,
            Long courseId,
            String fileType) {

        User user = getAuthenticatedUser(authentication);

        // Students must be enrolled; faculty must be assigned.
        getAuthorizedCourse(courseId, user);

        if (fileType == null || fileType.isBlank()) {
            throw new BadRequestException(
                    "Material type is required");
        }

        String normalizedType =
                fileType.trim().toUpperCase();

        if (!ALLOWED_FILE_TYPES.contains(normalizedType)) {
            throw new BadRequestException(
                    "Invalid material type");
        }

        return materialRepository
                .findByCourse_IdAndFileTypeAndActiveTrue(
                        courseId,
                        normalizedType)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // =========================================================
    // UPDATE MATERIAL
    // =========================================================

    public StudyMaterialDTO updateMaterial(
            Authentication authentication,
            Long materialId,
            String title,
            String description) {

        User user = getAuthenticatedUser(authentication);

        requireFacultyOrAdmin(user);

        StudyMaterial material =
                getAuthorizedMaterial(materialId, user);

        if (title == null || title.isBlank()) {
            throw new BadRequestException(
                    "Material title is required");
        }

        material.setTitle(title.trim());

        material.setDescription(
                description == null
                        ? ""
                        : description.trim());

        StudyMaterial updatedMaterial =
                materialRepository.save(material);

        return convertToDTO(updatedMaterial);
    }

    // =========================================================
    // SOFT DELETE MATERIAL
    // =========================================================

    public void deleteMaterial(
            Authentication authentication,
            Long materialId) {

        User user = getAuthenticatedUser(authentication);

        requireFacultyOrAdmin(user);

        StudyMaterial material =
                getAuthorizedMaterial(materialId, user);

        // Keep the record but hide it from active material lists.
        material.setActive(false);

        materialRepository.save(material);
    }

    // =========================================================
    // DTO CONVERSION
    // =========================================================

    private StudyMaterialDTO convertToDTO(
            StudyMaterial material) {

        return new StudyMaterialDTO(
                material.getId(),
                material.getCourse().getId(),
                material.getTitle(),
                material.getDescription(),
                material.getFileType(),
                material.getFileUrl(),
                material.getFileName(),
                material.getFileSize()
        );
    }
}
