package com.lms.controller;

import com.lms.dto.StudyMaterialDTO;
import com.lms.service.StudyMaterialService;
import com.lms.util.ApiResponseUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import com.lms.entity.StudyMaterial;
import com.lms.entity.User;
import com.lms.repository.StudyMaterialRepository;
import com.lms.repository.UserRepository;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/materials")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class StudyMaterialController {


private final StudyMaterialService materialService;
private final StudyMaterialRepository materialRepository;
private final UserRepository userRepository;

// =========================================================
// UPLOAD MATERIAL
// =========================================================

@PostMapping("/upload")
@PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
public ResponseEntity<?> uploadMaterial(
        Authentication authentication,
        @RequestParam Long courseId,
        @RequestParam String title,
        @RequestParam(required = false, defaultValue = "") String description,
        @RequestParam String fileType,
        @RequestParam("file") MultipartFile file) throws IOException {

    StudyMaterialDTO materialDTO =
            materialService.uploadMaterial(
                    authentication,
                    courseId,
                    title,
                    description,
                    fileType,
                    file);

    return ApiResponseUtil.created(
            "Material uploaded successfully",
            materialDTO);
}

// =========================================================
// GET MATERIAL BY ID
// =========================================================

@GetMapping("/{materialId}")
@PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
public ResponseEntity<?> getMaterialById(
        Authentication authentication,
        @PathVariable Long materialId) {

    StudyMaterialDTO materialDTO =
            materialService.getMaterialById(
                    authentication,
                    materialId);

    return ApiResponseUtil.success(
            "Material retrieved successfully",
            materialDTO);
}

// =========================================================
// GET MATERIALS BY COURSE
// =========================================================

@GetMapping("/course/{courseId}")
@PreAuthorize("hasAnyRole('FACULTY', 'ADMIN', 'STUDENT')")
public ResponseEntity<?> getCourseMaterials(
        Authentication authentication,
        @PathVariable Long courseId) {

    List<StudyMaterialDTO> materials =
            materialService.getCourseMaterials(
                    authentication,
                    courseId);

    return ApiResponseUtil.success(
            "Course materials retrieved successfully",
            materials);
}

// =========================================================
// GET MATERIALS BY COURSE AND TYPE
// =========================================================

@GetMapping("/course/{courseId}/type/{fileType}")
@PreAuthorize("hasAnyRole('FACULTY', 'ADMIN','STUDENT')")
public ResponseEntity<?> getCourseMaterialsByType(
        Authentication authentication,
        @PathVariable Long courseId,
        @PathVariable String fileType) {

    List<StudyMaterialDTO> materials =
            materialService.getCourseMaterialsByType(
                    authentication,
                    courseId,
                    fileType);

    return ApiResponseUtil.success(
            "Materials retrieved successfully",
            materials);
}

// =========================================================
// UPDATE MATERIAL
// =========================================================

@PutMapping("/{materialId}")
@PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
public ResponseEntity<?> updateMaterial(
        Authentication authentication,
        @PathVariable Long materialId,
        @RequestParam String title,
        @RequestParam(required = false, defaultValue = "") String description) {

    StudyMaterialDTO materialDTO =
            materialService.updateMaterial(
                    authentication,
                    materialId,
                    title,
                    description);

    return ApiResponseUtil.success(
            "Material updated successfully",
            materialDTO);
}

// =========================================================
// SOFT DELETE MATERIAL
// =========================================================

@DeleteMapping("/{materialId}")
@PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
public ResponseEntity<?> deleteMaterial(
        Authentication authentication,
        @PathVariable Long materialId) {

    materialService.deleteMaterial(
            authentication,
            materialId);

    return ApiResponseUtil.success(
            "Material deleted successfully");
}
@GetMapping("/{storedFileName}/download")
@PreAuthorize("hasAnyRole('FACULTY', 'ADMIN', 'STUDENT')")
public ResponseEntity<Resource> downloadMaterial(
        Authentication authentication,
        @PathVariable String storedFileName) throws IOException {

    // Validate authentication.
    if (authentication == null
            || !authentication.isAuthenticated()) {
        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Please log in to download this material");
    }

    // Validate the filename.
    if (storedFileName == null
            || storedFileName.isBlank()
            || storedFileName.contains("/")
            || storedFileName.contains("\\")
            || storedFileName.contains("..")) {

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid filename");
    }

    /*
     * Find the material using the exact stored filename.
     * The URL saved in the database is:
     * /api/materials/{storedFileName}/download
     */
    String expectedUrl =
            "/api/materials/" + storedFileName + "/download";

    StudyMaterial material = materialRepository.findAll()
            .stream()
            .filter(m -> Boolean.TRUE.equals(m.getActive()))
            .filter(m -> expectedUrl.equals(m.getFileUrl()))
            .findFirst()
            .orElseThrow(() ->
                    new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Active material not found"));

    /*
     * IMPORTANT:
     * Reuse the service authorization check.
     * Students must be enrolled in the course.
     * Faculty must be assigned to the course.
     * Admins can access active courses.
     */
    materialService.getMaterialById(
            authentication,
            material.getId());

    // Resolve the physical file path.
    Path uploadDirectory = Paths.get(
            "uploads", "materials")
            .toAbsolutePath()
            .normalize();

    Path filePath = uploadDirectory
            .resolve(storedFileName)
            .normalize();

    // Prevent path traversal and verify the file exists.
    if (!filePath.startsWith(uploadDirectory)
            || !java.nio.file.Files.exists(filePath)
            || !java.nio.file.Files.isRegularFile(filePath)) {

        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Physical file not found on the server");
    }

    Resource resource = new UrlResource(filePath.toUri());

    if (!resource.exists() || !resource.isReadable()) {
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "File cannot be read");
    }

    // Detect content type.
    String contentType =
            java.nio.file.Files.probeContentType(filePath);

    if (contentType == null) {
        contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    // Use the original filename for the downloaded file.
    String originalFileName = material.getFileName();

    if (originalFileName == null || originalFileName.isBlank()) {
        originalFileName = storedFileName;
    }

    return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\""
                            + originalFileName.replace("\"", "")
                            + "\"")
            .body(resource);
}
}
