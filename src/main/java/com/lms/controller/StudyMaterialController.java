package com.lms.controller;

import com.lms.dto.StudyMaterialDTO;
import com.lms.service.StudyMaterialService;
import com.lms.util.ApiResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/materials")
@CrossOrigin(origins = "*")
public class StudyMaterialController {

    @Autowired
    private StudyMaterialService materialService;

    @PostMapping("/upload")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<?> uploadMaterial(
            @RequestParam Long courseId,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam String fileType,
            @RequestParam MultipartFile file) throws IOException {
        StudyMaterialDTO materialDTO = materialService.uploadMaterial(courseId, title, description, fileType, file);
        return ApiResponseUtil.created("Material uploaded successfully", materialDTO);
    }

    @GetMapping("/{materialId}")
    public ResponseEntity<?> getMaterialById(@PathVariable Long materialId) {
        StudyMaterialDTO materialDTO = materialService.getMaterialById(materialId);
        return ApiResponseUtil.success("Material retrieved successfully", materialDTO);
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getCourseMaterials(@PathVariable Long courseId) {
        List<StudyMaterialDTO> materials = materialService.getCourseMaterials(courseId);
        return ApiResponseUtil.success("Course materials retrieved", materials);
    }

    @GetMapping("/course/{courseId}/type/{fileType}")
    public ResponseEntity<?> getCourseMaterialsByType(@PathVariable Long courseId, @PathVariable String fileType) {
        List<StudyMaterialDTO> materials = materialService.getCourseMaterialsByType(courseId, fileType);
        return ApiResponseUtil.success("Materials retrieved by type", materials);
    }

    @PutMapping("/{materialId}")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<?> updateMaterial(
            @PathVariable Long materialId,
            @RequestParam String title,
            @RequestParam String description) {
        StudyMaterialDTO materialDTO = materialService.updateMaterial(materialId, title, description);
        return ApiResponseUtil.success("Material updated successfully", materialDTO);
    }

    @DeleteMapping("/{materialId}")
    @PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteMaterial(@PathVariable Long materialId) {
        materialService.deleteMaterial(materialId);
        return ApiResponseUtil.success("Material deleted successfully");
    }
}
