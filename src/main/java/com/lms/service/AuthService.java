package com.lms.service;

import com.lms.dto.AuthRequest;
import com.lms.dto.AuthResponse;
import com.lms.dto.SignupRequest;
import com.lms.dto.UserDTO;
import com.lms.entity.Course;
import com.lms.entity.Enrollment;
import com.lms.entity.Role;
import com.lms.entity.User;
import com.lms.exception.BadRequestException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.CourseRepository;
import com.lms.repository.EnrollmentRepository;
import com.lms.repository.RoleRepository;
import com.lms.repository.UserRepository;
import com.lms.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.lms.dto.StudentProfileDTO;
import com.lms.service.EmailService;

@Service
@Transactional
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
private CourseRepository courseRepository;

@Autowired
private EnrollmentRepository enrollmentRepository;

@Autowired
private EmailService emailService;


    public UserDTO signup(SignupRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Email already registered");
        }
System.out.println("ROLE ID = " + request.getRoleId());

Role role = roleRepository.findById(request.getRoleId())
        .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setRole(role);
        user.setActive(true);
User savedUser = userRepository.save(user);

if (role.getName().equals("STUDENT")) {

    Course course = courseRepository.findById(
            request.getCourseId()
        ).orElseThrow(
            () -> new ResourceNotFoundException("Course not found")
        );

    Enrollment enrollment = new Enrollment();

    enrollment.setStudent(savedUser);
    enrollment.setCourse(course);
    enrollment.setStatus("ACTIVE");
    enrollment.setCompletionPercentage(0.0);

    enrollment.setEnrollmentDate(LocalDateTime.now());

    enrollment.setDueDate(
        LocalDateTime.now().plusDays(course.getDurationDays())
    );

    enrollmentRepository.save(enrollment);

    // Send welcome email only to students
    emailService.sendWelcomeEmail(
        savedUser.getEmail(),
        savedUser.getFirstName()
    );
}

return convertToDTO(savedUser);

    }
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getActive()) {
            throw new BadRequestException("User account is inactive");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );


        String token = jwtTokenProvider.generateToken(authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.User
                ? (org.springframework.security.core.userdetails.User) authentication.getPrincipal()
                : org.springframework.security.core.userdetails.User.builder()
                    .username(user.getEmail())
                    .password(user.getPassword())
                    .authorities("ROLE_" + user.getRole().getName())
                    .build());

        String refreshToken = jwtTokenProvider.generateRefreshToken(
                org.springframework.security.core.userdetails.User.builder()
                    .username(user.getEmail())
                    .password(user.getPassword())
                    .authorities("ROLE_" + user.getRole().getName())
                    .build());

        AuthResponse response = new AuthResponse();

response.setToken(token);
response.setRefreshToken(refreshToken);
response.setSuccess(true);
response.setMessage("Login successful");

response.setUserId(user.getId());
response.setFirstName(user.getFirstName());
response.setRole(user.getRole().getName());
        return response;
    }

    public UserDTO getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return convertToDTO(user);
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<UserDTO> getUsersByRole(String roleName) {
        return userRepository.findByRole_Name(roleName).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public UserDTO updateUserProfile(Long userId, SignupRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());

        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(false);
        userRepository.save(user);
    }

    private UserDTO convertToDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getAddress(),
                user.getRole().getName(),
                user.getActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
        
    }
    // ==========================================
// CHANGE PASSWORD
// ==========================================
public void changePassword(
        String userId,
        String currentPassword,
        String newPassword,
        String confirmPassword) {

    if (!StringUtils.hasText(userId)) {
        throw new BadRequestException("User ID is required");
    }

    if (!StringUtils.hasText(currentPassword)
            || !StringUtils.hasText(newPassword)
            || !StringUtils.hasText(confirmPassword)) {

        throw new BadRequestException(
                "All password fields are required"
        );
    }

    if (newPassword.length() < 8) {
        throw new BadRequestException(
                "New password must contain at least 8 characters"
        );
    }

    if (!newPassword.equals(confirmPassword)) {
        throw new BadRequestException(
                "New password and confirmation do not match"
        );
    }

    Long id;

    try {
        id = Long.valueOf(userId);
    } catch (NumberFormatException exception) {
        throw new BadRequestException("Invalid user ID");
    }

    User user = userRepository.findById(id)
            .orElseThrow(
                    () -> new ResourceNotFoundException("User not found")
            );

    if (!passwordEncoder.matches(
            currentPassword,
            user.getPassword())) {

        throw new BadRequestException(
                "Current password is incorrect"
        );
    }

    if (passwordEncoder.matches(
            newPassword,
            user.getPassword())) {

        throw new BadRequestException(
                "New password must be different from current password"
        );
    }

    user.setPassword(passwordEncoder.encode(newPassword));

    userRepository.save(user);
}
// ==========================================
// GET COMPLETE STUDENT PROFILE
// ==========================================

public StudentProfileDTO getCompleteStudentProfile(Long userId) {

    User user = userRepository.findById(userId)
            .orElseThrow(
                    () -> new ResourceNotFoundException("User not found")
            );

    UserDTO userDTO = convertToDTO(user);

    List<StudentProfileDTO.CourseInfo> courses =
            enrollmentRepository.findByStudent_Id(userId)
                    .stream()
                    .map(enrollment -> {

                        Course course = enrollment.getCourse();

                        return new StudentProfileDTO.CourseInfo(
                                enrollment.getId(),
                                course.getId(),
                                course.getTitle(),
                                course.getCourseCode(),
                                course.getDescription(),
                                course.getDurationDays(),
                                course.getTotalFees(),
                                enrollment.getEnrollmentDate(),
                                enrollment.getDueDate(),
                                enrollment.getStatus(),
                                enrollment.getCompletionPercentage()
                        );
                    })
                    .collect(Collectors.toList());

    return new StudentProfileDTO(userDTO, courses);
}
}
