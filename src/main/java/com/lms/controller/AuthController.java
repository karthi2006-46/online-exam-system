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
import org.springframework.security.core.Authentication;
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
            @PathVariable Long userId,
            Authentication authentication) {

        authService.requireSelfOrAdmin(userId, authentication);

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
            @RequestBody SignupRequest request,
            Authentication authentication) {

        authService.requireSelfOrAdmin(userId, authentication);

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
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");
        String confirmPassword = request.get("confirmPassword");

        // Always use the JWT identity. A userId in the body is ignored.
        Long currentUserId = authService.getCurrentUserId(authentication);

        authService.changePassword(
                String.valueOf(currentUserId),
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
        @PathVariable Long userId,
        Authentication authentication) {

    authService.requireSelfOrAdmin(userId, authentication);

    StudentProfileDTO profile =
            authService.getCompleteStudentProfile(userId);

    return ApiResponseUtil.success(
            "Student profile and course information retrieved successfully",
            profile
    );
}
// ==========================================
// FORGOT PASSWORD - SEND OTP
// ==========================================

@PostMapping("/forgot-password")
public ResponseEntity<?> forgotPassword(
        @RequestBody Map<String, String> request) {

    authService.sendPasswordResetOtp(request.get("email"));

    return ResponseEntity.ok(
            Map.of(
                    "success", true,
                    "message",
                    "If the account exists, a password reset OTP has been sent."
            )
    );
}


// ==========================================
// RESET PASSWORD - VERIFY OTP
// ==========================================

@PostMapping("/reset-password")
public ResponseEntity<?> resetPassword(
        @RequestBody Map<String, String> request) {

    authService.resetPassword(
            request.get("email"),
            request.get("otp"),
            request.get("newPassword"),
            request.get("confirmPassword")
    );

    return ResponseEntity.ok(
            Map.of(
                    "success", true,
                    "message", "Password reset successfully"
            )
    );
}

}