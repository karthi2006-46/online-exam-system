package com.lms.service;

import com.lms.dto.CodingChallengeDTO;
import com.lms.dto.CodingChallengeRequest;
import com.lms.entity.CodingChallenge;
import com.lms.entity.Course;
import com.lms.entity.User;
import com.lms.exception.BadRequestException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.exception.UnauthorizedException;
import com.lms.repository.CodingChallengeProgressRepository;
import com.lms.repository.CodingChallengeRepository;
import com.lms.repository.CodingSubmissionRepository;
import com.lms.repository.CourseRepository;
import com.lms.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for coding challenges.
 *
 * Student endpoints only ever see active challenges and never receive
 * the owning course/faculty entities (which would leak the faculty's
 * password hash). Faculty endpoints additionally verify that the
 * challenge belongs to a course the faculty member owns.
 */
@Service
@RequiredArgsConstructor
public class CodingChallengeService {

    private static final List<String> ALLOWED_DIFFICULTIES =
            List.of("EASY", "MEDIUM", "HARD");

    private static final int MAX_TITLE_LENGTH = 255;

    private final CodingChallengeRepository challengeRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CodingSubmissionRepository codingSubmissionRepository;
    private final CodingChallengeProgressRepository progressRepository;

    // ==========================================
    // STUDENT SIDE
    // ==========================================

    @Transactional(readOnly = true)
    public List<CodingChallengeDTO> getActiveChallengesForCourse(Long courseId) {

        if (courseId == null) {
            throw new BadRequestException("Course ID is required");
        }

        return challengeRepository.findByCourse_IdAndActiveTrue(courseId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public CodingChallengeDTO getActiveChallenge(Long challengeId) {

        return challengeRepository.findById(challengeId)
                .filter(challenge -> Boolean.TRUE.equals(challenge.getActive()))
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active coding challenge not found"
                ));
    }

    // ==========================================
    // FACULTY SIDE
    // ==========================================

    @Transactional(readOnly = true)
    public List<CodingChallengeDTO> getCourseChallengesForFaculty(
            Authentication authentication,
            Long courseId
    ) {

        Course course = requireOwnedCourse(
                currentUser(authentication),
                courseId
        );

        return challengeRepository.findByCourse_Id(course.getId())
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public CodingChallengeDTO create(
            Authentication authentication,
            CodingChallengeRequest request
    ) {

        if (request == null) {
            throw new BadRequestException("Challenge details are required");
        }

        User faculty = currentUser(authentication);

        Course course = requireOwnedCourse(faculty, request.getCourseId());

        CodingChallenge challenge = new CodingChallenge();
        challenge.setCourse(course);

        applyRequest(challenge, request, true);

        if (challenge.getActive() == null) {
            challenge.setActive(true);
        }

        return toDTO(challengeRepository.save(challenge));
    }

    @Transactional
    public CodingChallengeDTO update(
            Authentication authentication,
            Long challengeId,
            CodingChallengeRequest request
    ) {

        if (request == null) {
            throw new BadRequestException("Challenge details are required");
        }

        User faculty = currentUser(authentication);
        CodingChallenge challenge = findChallenge(challengeId);

        // The faculty member must own the course the challenge currently
        // belongs to, and the course they are moving it to (if any).
        requireOwnedCourse(faculty, challenge.getCourse().getId());

        if (request.getCourseId() != null
                && !request.getCourseId().equals(challenge.getCourse().getId())) {

            challenge.setCourse(
                    requireOwnedCourse(faculty, request.getCourseId())
            );
        }

        applyRequest(challenge, request, false);

        return toDTO(challengeRepository.save(challenge));
    }

    @Transactional
    public CodingChallengeDTO setActive(
            Authentication authentication,
            Long challengeId,
            Boolean active
    ) {

        if (active == null) {
            throw new BadRequestException("The 'active' value is required");
        }

        User faculty = currentUser(authentication);
        CodingChallenge challenge = findChallenge(challengeId);

        requireOwnedCourse(faculty, challenge.getCourse().getId());

        challenge.setActive(active);

        return toDTO(challengeRepository.save(challenge));
    }

    @Transactional
    public void delete(Authentication authentication, Long challengeId) {

        User faculty = currentUser(authentication);
        CodingChallenge challenge = findChallenge(challengeId);

        requireOwnedCourse(faculty, challenge.getCourse().getId());

        boolean hasSubmissions =
                codingSubmissionRepository.countByChallenge_Id(challengeId) > 0;

        boolean hasProgress =
                progressRepository.countByChallenge_Id(challengeId) > 0;

        if (hasSubmissions || hasProgress) {
            throw new BadRequestException(
                    "This challenge already has student activity. "
                            + "Deactivate it instead of deleting it."
            );
        }

        challengeRepository.delete(challenge);
    }

    // ==========================================
    // VALIDATION / MAPPING HELPERS
    // ==========================================

    private void applyRequest(
            CodingChallenge challenge,
            CodingChallengeRequest request,
            boolean creating
    ) {

        String title = trimToNull(request.getTitle());

        if (title == null) {
            throw new BadRequestException("Challenge title is required");
        }

        if (title.length() > MAX_TITLE_LENGTH) {
            throw new BadRequestException(
                    "Challenge title must be at most "
                            + MAX_TITLE_LENGTH + " characters"
            );
        }

        challenge.setTitle(title);

        String difficulty = trimToNull(request.getDifficulty());

        if (difficulty == null) {
            if (creating) {
                challenge.setDifficulty("EASY");
            }
        } else {
            difficulty = difficulty.toUpperCase();

            if (!ALLOWED_DIFFICULTIES.contains(difficulty)) {
                throw new BadRequestException(
                        "Difficulty must be EASY, MEDIUM or HARD"
                );
            }

            challenge.setDifficulty(difficulty);
        }

        if (request.getMarks() != null) {

            if (request.getMarks() < 0) {
                throw new BadRequestException("Marks cannot be negative");
            }

            challenge.setMarks(request.getMarks());
        }

        if (request.getActive() != null) {
            challenge.setActive(request.getActive());
        }

        challenge.setDescription(trimToNull(request.getDescription()));
        challenge.setProblemStatement(trimToNull(request.getProblemStatement()));
        challenge.setStarterCode(trimToNull(request.getStarterCode()));
        challenge.setExpectedOutput(trimToNull(request.getExpectedOutput()));
        challenge.setSampleInput(trimToNull(request.getSampleInput()));
        challenge.setSampleOutput(trimToNull(request.getSampleOutput()));
        challenge.setConstraints(trimToNull(request.getConstraints()));
    }

    private CodingChallenge findChallenge(Long challengeId) {

        return challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Coding challenge not found"
                ));
    }

    private User currentUser(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserDetails details)) {

            throw new UnauthorizedException("Authentication required");
        }

        return userRepository.findByEmail(details.getUsername())
                .orElseThrow(() -> new UnauthorizedException(
                        "Authenticated user not found"
                ));
    }

    private Course requireOwnedCourse(User user, Long courseId) {

        if (courseId == null) {
            throw new BadRequestException("Course ID is required");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Course not found"
                ));

        boolean admin = user.getRole() != null
                && "ADMIN".equalsIgnoreCase(user.getRole().getName());

        boolean owner = course.getFaculty() != null
                && course.getFaculty().getId().equals(user.getId());

        if (!admin && !owner) {
            throw new AccessDeniedException(
                    "You can only manage challenges for your own courses"
            );
        }

        return course;
    }

    private CodingChallengeDTO toDTO(CodingChallenge challenge) {

        Course course = challenge.getCourse();

        return CodingChallengeDTO.builder()
                .id(challenge.getId())
                .courseId(course != null ? course.getId() : null)
                .courseTitle(course != null ? course.getTitle() : null)
                .title(challenge.getTitle())
                .description(challenge.getDescription())
                .problemStatement(challenge.getProblemStatement())
                .starterCode(challenge.getStarterCode())
                .expectedOutput(challenge.getExpectedOutput())
                .sampleInput(challenge.getSampleInput())
                .sampleOutput(challenge.getSampleOutput())
                .constraints(challenge.getConstraints())
                .difficulty(challenge.getDifficulty())
                .marks(challenge.getMarks())
                .active(challenge.getActive())
                .build();
    }

    private String trimToNull(String value) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }
}
