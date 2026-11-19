package com.lms.controller;

import com.lms.dto.MaterialViewProgressRequest;
import com.lms.entity.User;
import com.lms.repository.UserRepository;
import com.lms.service.MaterialProgressService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/material-progress")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class MaterialProgressController {

    private final MaterialProgressService progressService;
    private final UserRepository userRepository;

    // ======================================
    // GET COMPLETED MATERIALS
    // ======================================

    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getCompletedMaterials(
            @PathVariable Long studentId,
            Authentication authentication) {

        User student = getAuthenticatedStudent(authentication);

        if (!student.getId().equals(studentId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You cannot access another student's progress");
        }

        return ResponseEntity.ok(
                progressService.getCompletedMaterialIds(studentId)
        );
    }

    // ======================================
    // PDF PROGRESS
    // ======================================

    @PostMapping("/pdf/{materialId}")
    public ResponseEntity<?> updatePdfProgress(
            @PathVariable Long materialId,
            @RequestBody MaterialViewProgressRequest request,
            Authentication authentication) {

        User student = getAuthenticatedStudent(authentication);

        return ResponseEntity.ok(
                progressService.recordPdfProgress(
                        student.getId(),
                        materialId,
                        request
                )
        );
    }

    // ======================================
    // VIDEO PROGRESS
    // ======================================

    @PostMapping("/video/{materialId}")
    public ResponseEntity<?> updateVideoProgress(
            @PathVariable Long materialId,
            @RequestBody MaterialViewProgressRequest request,
            Authentication authentication) {

        User student = getAuthenticatedStudent(authentication);

        return ResponseEntity.ok(
                progressService.recordVideoProgress(
                        student.getId(),
                        materialId,
                        request
                )
        );
    }

    // ======================================
    // TEXT PROGRESS
    // ======================================

    @PostMapping("/text/{materialId}")
    public ResponseEntity<?> updateTextProgress(
            @PathVariable Long materialId,
            @RequestBody MaterialViewProgressRequest request,
            Authentication authentication) {

        User student = getAuthenticatedStudent(authentication);

        return ResponseEntity.ok(
                progressService.recordTextProgress(
                        student.getId(),
                        materialId,
                        request
                )
        );
    }

    // ======================================
    // COMPLETE MATERIAL
    // ======================================

    @PostMapping("/complete/{materialId}")
    public ResponseEntity<?> completeMaterial(
            @PathVariable Long materialId,
            Authentication authentication) {

        User student = getAuthenticatedStudent(authentication);

        return ResponseEntity.ok(
                progressService.markCompleted(
                        student.getId(),
                        materialId
                )
        );
    }

    // ======================================
    // GET AUTHENTICATED STUDENT
    // ======================================

    private User getAuthenticatedStudent(
            Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserDetails userDetails)) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid authenticated user"
            );
        }

        return userRepository.findByEmail(
                userDetails.getUsername()
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User not found"
        ));
    }
}
