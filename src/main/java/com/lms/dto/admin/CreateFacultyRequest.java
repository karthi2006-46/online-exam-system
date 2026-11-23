package com.lms.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin payload for creating a faculty account.
 */
@Data
@NoArgsConstructor
public class CreateFacultyRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    /**
     * Optional. When blank and generatePassword is true,
     * the server generates a temporary password.
     */
    @Size(min = 8, max = 100, message = "Password must contain at least 8 characters")
    private String password;

    private boolean generatePassword = true;

    private String phone;

    @Size(max = 500, message = "Address is too long")
    private String address;

    private List<Long> courseIds = new ArrayList<>();
}
