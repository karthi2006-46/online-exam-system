package com.lms.service;

import com.lms.dto.StudyMaterialDTO;
import com.lms.entity.Course;
import com.lms.entity.StudyMaterial;
import com.lms.exception.BadRequestException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.CourseRepository;
import com.lms.repository.StudyMaterialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudyMaterialService {

    @Autowired
    private StudyMaterialRepository materialRepository;

    @Autowired
    private CourseRepository courseRepository;

    private static final String UPLOAD_DIR = "uploads/materials/";

    public StudyMaterialDTO uploadMaterial(Long courseId, String title, String description,
                                           String fileType, MultipartFile file) throws IOException {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        if (file.isEmpty()) {
            throw new BadRequestException("File cannot be empty");
        }

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(UPLOAD_DIR).resolve(fileName);
        Files.createDirectories(filePath.getParent());
        Files.copy(file.getInputStream(), filePath);

        StudyMaterial material = new StudyMaterial();
        material.setCourse(course);
        material.setTitle(title);
        material.setDescription(description);
        material.setFileType(fileType);
        material.setFileUrl("/uploads/materials/" + fileName);
        material.setFileName(file.getOriginalFilename());
        material.setFileSize(file.getSize());
        material.setActive(true);

        StudyMaterial savedMaterial = materialRepository.save(material);
        return convertToDTO(savedMaterial);
    }

    public StudyMaterialDTO getMaterialById(Long materialId) {
        StudyMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Study material not found"));
        return convertToDTO(material);
    }

    public List<StudyMaterialDTO> getCourseMaterials(Long courseId) {
        return materialRepository.findByCourse_Id(courseId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<StudyMaterialDTO> getCourseMaterialsByType(Long courseId, String fileType) {
        return materialRepository.findByCourse_IdAndFileType(courseId, fileType).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public StudyMaterialDTO updateMaterial(Long materialId, String title, String description) {
        StudyMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Study material not found"));

        material.setTitle(title);
        material.setDescription(description);

        StudyMaterial updatedMaterial = materialRepository.save(material);
        return convertToDTO(updatedMaterial);
    }

    public void deleteMaterial(Long materialId) {
        StudyMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Study material not found"));
        material.setActive(false);
        materialRepository.save(material);
    }

    private StudyMaterialDTO convertToDTO(StudyMaterial material) {
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
