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
import com.lms.exception.UnauthorizedException;
import org.springframework.security.access.AccessDeniedException;
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
import com.lms.entity.PasswordResetOtp;
import com.lms.repository.PasswordResetOtpRepository;

import java.security.SecureRandom;

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
@Autowired
private PasswordResetOtpRepository passwordResetOtpRepository;

@Autowired
private AdminSettingsService adminSettingsService;

private final SecureRandom secureRandom = new SecureRandom();


    public UserDTO signup(SignupRequest request) {
        if (!adminSettingsService.isStudentSignupAllowed()) {
            throw new BadRequestException(
                    "Student registration is currently disabled by the administrator");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Email already registered");
        }

        // Public registration always creates a STUDENT account.
        // Any roleId sent by the client is ignored on purpose.
        Role role = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student role is not configured"
                ));

        if (request.getCourseId() == null) {
            throw new BadRequestException("Course is required");
        }

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Course not found"
                ));

        if (!Boolean.TRUE.equals(course.getActive())) {
            throw new BadRequestException(
                    "Selected course is not available"
            );
        }

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

        // Send welcome email to the new student
        emailService.sendWelcomeEmail(
                savedUser.getEmail(),
                savedUser.getFirstName()
        );

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

    /**
     * Resolves the authenticated user's id from the JWT principal.
     * Never trust a user id supplied by the frontend.
     */
    public Long getCurrentUserId(Authentication authentication) {
        return currentUser(authentication).getId();
    }

    /**
     * Allows access only to the authenticated user's own record,
     * or to an ADMIN account.
     */
    public void requireSelfOrAdmin(
            Long targetUserId,
            Authentication authentication) {

        if (targetUserId == null) {
            throw new BadRequestException("User ID is required");
        }

        User current = currentUser(authentication);

        boolean isAdmin = current.getRole() != null
                && current.getRole().getName() != null
                && current.getRole().getName().equalsIgnoreCase("ADMIN");

        if (!isAdmin && !current.getId().equals(targetUserId)) {
            throw new AccessDeniedException(
                    "You can only access your own profile"
            );
        }
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {

            throw new UnauthorizedException(
                    "Authentication required"
            );
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException(
                        "Authenticated user not found"
                ));
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
// ==========================================
// SEND PASSWORD RESET OTP
// ==========================================

public void sendPasswordResetOtp(String email) {

    if (!StringUtils.hasText(email)) {
        throw new BadRequestException("Email is required");
    }

    email = email.trim().toLowerCase();

    // Do not reveal whether the account exists.
    User user = userRepository.findByEmail(email).orElse(null);

    if (user == null || !Boolean.TRUE.equals(user.getActive())) {
        return;
    }

    // Prevent repeated OTP requests within 60 seconds.
    PasswordResetOtp existing =
            passwordResetOtpRepository.findByEmail(email).orElse(null);

    if (existing != null &&
            existing.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(60))) {
        throw new BadRequestException(
                "Please wait before requesting another OTP"
        );
    }

    passwordResetOtpRepository.deleteByEmail(email);

    int otpNumber = 100000 + secureRandom.nextInt(900000);
    String otp = String.valueOf(otpNumber);

    PasswordResetOtp resetOtp = new PasswordResetOtp();

    resetOtp.setEmail(email);
    resetOtp.setOtpHash(passwordEncoder.encode(otp));
    resetOtp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
    resetOtp.setCreatedAt(LocalDateTime.now());
    resetOtp.setAttempts(0);

    passwordResetOtpRepository.save(resetOtp);

    emailService.sendPasswordResetOtp(email, otp);
}


// ==========================================
// VERIFY OTP AND RESET PASSWORD
// ==========================================

public void resetPassword(
        String email,
        String otp,
        String newPassword,
        String confirmPassword) {

    if (!StringUtils.hasText(email)
            || !StringUtils.hasText(otp)
            || !StringUtils.hasText(newPassword)
            || !StringUtils.hasText(confirmPassword)) {

        throw new BadRequestException("All fields are required");
    }

    email = email.trim().toLowerCase();

    if (!newPassword.equals(confirmPassword)) {
        throw new BadRequestException(
                "New password and confirmation do not match"
        );
    }

    if (newPassword.length() < 8) {
        throw new BadRequestException(
                "Password must contain at least 8 characters"
        );
    }

    PasswordResetOtp resetOtp =
            passwordResetOtpRepository.findByEmail(email)
                    .orElseThrow(() ->
                            new BadRequestException("Invalid or expired OTP"));

    if (resetOtp.getExpiresAt().isBefore(LocalDateTime.now())) {
        passwordResetOtpRepository.deleteByEmail(email);
        throw new BadRequestException("OTP has expired. Request a new OTP.");
    }

    if (resetOtp.getAttempts() >= 5) {
        passwordResetOtpRepository.deleteByEmail(email);
        throw new BadRequestException(
                "Maximum OTP attempts exceeded. Request a new OTP."
        );
    }

    if (!passwordEncoder.matches(otp, resetOtp.getOtpHash())) {

        resetOtp.setAttempts(resetOtp.getAttempts() + 1);
        passwordResetOtpRepository.save(resetOtp);

        throw new BadRequestException("Incorrect OTP");
    }

    User user = userRepository.findByEmail(email)
            .orElseThrow(() ->
                    new ResourceNotFoundException("User not found"));

    if (passwordEncoder.matches(newPassword, user.getPassword())) {
        throw new BadRequestException(
                "New password must be different from the current password"
        );
    }

    user.setPassword(passwordEncoder.encode(newPassword));

    userRepository.save(user);

    // OTP cannot be reused.
    passwordResetOtpRepository.deleteByEmail(email);
}
}
