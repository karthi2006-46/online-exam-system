package com.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudyMaterialDTO {
    private Long id;
    private Long courseId;
    private String title;
    private String description;
    private String fileType;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
}
