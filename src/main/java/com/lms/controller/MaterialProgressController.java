package com.lms.controller;

import com.lms.entity.User;
import com.lms.repository.UserRepository;
import com.lms.service.StudyMaterialProgressService;

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
public class MaterialProgressController {

    private final StudyMaterialProgressService progressService;
    private final UserRepository userRepository;

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getCompletedMaterials(
            @PathVariable Long studentId,
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Authentication required");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserDetails userDetails)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid authenticated user");
        }

        User student = userRepository.findByEmail(
                userDetails.getUsername()
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User not found"
        ));

        if (!student.getId().equals(studentId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You cannot access another student's progress");
        }

        return ResponseEntity.ok(
                progressService.getCompletedMaterialIds(studentId)
        );
    }
}