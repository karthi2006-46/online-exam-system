package com.lms.controller;

import com.lms.entity.StudyMaterialProgress;
import com.lms.service.StudyMaterialProgressService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/material-progress")
@RequiredArgsConstructor
public class StudyMaterialProgressController {

    private final StudyMaterialProgressService progressService;

    @PostMapping("/complete/{materialId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> markCompleted(
            @PathVariable Long materialId,
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()) {

            return ResponseEntity.status(401).body(
                    Map.of("message", "Authentication required")
            );
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserDetails userDetails)) {

            return ResponseEntity.status(401).body(
                    Map.of("message", "Invalid authenticated user")
            );
        }

        StudyMaterialProgress progress =
                progressService.markCompletedByEmail(
                        userDetails.getUsername(),
                        materialId
                );

        return ResponseEntity.ok(
                Map.of(
                        "message", "Study material marked as completed",
                        "materialId", progress.getStudyMaterial().getId(),
                        "completed", progress.getCompleted()
                )
        );
    }
}