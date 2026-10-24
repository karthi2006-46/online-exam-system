package com.lms.controller;

import com.lms.dto.AuthRequest;
import com.lms.dto.AuthResponse;
import com.lms.dto.SignupRequest;
import com.lms.dto.UserDTO;
import com.lms.service.AuthService;
import com.lms.util.ApiResponseUtil;
import com.lms.dto.StudentProfileDTO;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "http://localhost:8080")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    // ==========================================
    // SIGNUP
    // ==========================================
    @PostMapping("/signup")
    public ResponseEntity<?> signup(
            @Valid @RequestBody SignupRequest request) {

        UserDTO userDTO = authService.signup(request);

        return ApiResponseUtil.created(
                "User registered successfully",
                userDTO
        );
    }

    // ==========================================
    // LOGIN
    // ==========================================
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody AuthRequest request) {

        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // GET STUDENT PROFILE
    // ==========================================
    @GetMapping("/profile/{userId}")
    public ResponseEntity<?> getUserProfile(
            @PathVariable Long userId) {

        UserDTO userDTO = authService.getUserProfile(userId);

        return ApiResponseUtil.success(
                "User profile retrieved successfully",
                userDTO
        );
    }

    // ==========================================
    // UPDATE STUDENT PROFILE
    // ==========================================
    @PutMapping("/profile/{userId}")
    public ResponseEntity<?> updateUserProfile(
            @PathVariable Long userId,
            @RequestBody SignupRequest request) {

        UserDTO updatedUser =
                authService.updateUserProfile(userId, request);

        return ApiResponseUtil.success(
                "Profile updated successfully",
                updatedUser
        );
    }

    // ==========================================
    // CHANGE PASSWORD
    // ==========================================
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody Map<String, String> request) {

        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");
        String confirmPassword = request.get("confirmPassword");

        authService.changePassword(
                request.get("userId"),
                currentPassword,
                newPassword,
                confirmPassword
        );

        return ApiResponseUtil.success(
                "Password changed successfully",
                null
        );
 
    }
    // ==========================================
// GET COMPLETE STUDENT PROFILE AND COURSES
// ==========================================

@GetMapping("/profile/{userId}/complete")
public ResponseEntity<?> getCompleteStudentProfile(
        @PathVariable Long userId) {

    StudentProfileDTO profile =
            authService.getCompleteStudentProfile(userId);

    return ApiResponseUtil.success(
            "Student profile and course information retrieved successfully",
            profile
    );
}
}