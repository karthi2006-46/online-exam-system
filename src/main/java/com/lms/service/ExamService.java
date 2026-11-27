package com.lms.service;

import com.lms.dto.ExamDTO;
import com.lms.entity.Course;
import com.lms.entity.Exam;
import com.lms.entity.User;
import com.lms.exception.BadRequestException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.CourseRepository;
import com.lms.repository.ExamAttemptRepository;
import com.lms.repository.ExamRepository;
import com.lms.repository.ResultRepository;
import com.lms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lms.repository.QuestionRepository;
import com.lms.entity.Question;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExamService {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private CourseRepository courseRepository;
@Autowired
private QuestionRepository questionRepository;
@Autowired
private ExamAttemptRepository examAttemptRepository;
@Autowired
private ResultRepository resultRepository;
@Autowired
private UserRepository userRepository;
    public ExamDTO createExam(ExamDTO request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        if (request.getPassingMarks() > request.getTotalMarks()) {
            throw new BadRequestException("Passing marks cannot be greater than total marks");
        }

        Exam exam = new Exam();
        exam.setCourse(course);
        exam.setTitle(request.getTitle());
        exam.setDescription(request.getDescription());
        exam.setTotalQuestions(request.getTotalQuestions());
        exam.setTotalMarks(request.getTotalMarks());
        exam.setPassingMarks(request.getPassingMarks());
        exam.setDurationMinutes(request.getDurationMinutes());
        exam.setActive(true);
        exam.setPublished(false);

        Exam savedExam = examRepository.save(exam);
        return convertToDTO(savedExam);
    }

    public ExamDTO getExamById(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found"));
        return convertToDTO(exam);
    }

    public List<ExamDTO> getCourseExams(Long courseId) {
        return examRepository.findByCourse_IdAndActiveTrue(courseId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ExamDTO> getPublishedExams(Long courseId) {
        return examRepository
                .findByCourse_IdAndActiveTrueAndPublishedTrue(courseId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

public ExamDTO publishExam(Long examId) {

    Exam exam = examRepository.findById(examId)
            .orElseThrow(() ->
                    new ResourceNotFoundException("Exam not found"));

    // Count questions directly from the database
    long questionCount = questionRepository.countByExam_Id(examId);

    if (questionCount == 0) {
        throw new BadRequestException(
                "Cannot publish exam without questions");
    }

    // Validate question count
    if (questionCount != exam.getTotalQuestions()) {
        throw new BadRequestException(
                "Exam requires " + exam.getTotalQuestions()
                + " questions, but only " + questionCount
                + " questions are available");
    }

    // Validate total marks
    int actualTotalMarks = questionRepository
            .findByExam_Id(examId)
            .stream()
            .mapToInt(Question::getMarks)
            .sum();

    if (actualTotalMarks != exam.getTotalMarks()) {
        throw new BadRequestException(
                "Exam total marks are " + exam.getTotalMarks()
                + ", but the questions add up to "
                + actualTotalMarks);
    }

    exam.setPublished(true);

    Exam updatedExam = examRepository.save(exam);

    return convertToDTO(updatedExam);
}

    public ExamDTO unpublishExam(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found"));
        exam.setPublished(false);
        Exam updatedExam = examRepository.save(exam);
        return convertToDTO(updatedExam);
    }

    /**
     * Permanently deletes an exam.
     *
     * <p>Safety / data-retention rules:
     * <ul>
     *   <li>Only an admin, or faculty who own the exam's course, may delete.</li>
     *   <li>Never destroy student data: if the exam has any
     *       {@link com.lms.entity.ExamAttempt} or {@link com.lms.entity.Result}
     *       rows the deletion is rejected. Unpublish it instead.</li>
     *   <li>Faculty-owned {@link Question}s are removed with the exam; no
     *       unsafe cascading to student attempts/results occurs (there are
     *       none).</li>
     * </ul>
     */
    @Transactional
    public void deleteExam(Long examId, Authentication authentication) {

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Exam not found"));

        requireCanManageExam(authentication, exam);

        long attempts = examAttemptRepository.countByExam_Id(examId);
        long results = resultRepository.countByExam_Id(examId);

        if (attempts > 0 || results > 0) {
            throw new BadRequestException(
                    "This exam has student attempts or results and cannot "
                            + "be deleted. Unpublish it instead.");
        }

        /*
         * Faculty-owned questions reference this exam. There is no student
         * data on them, but we must physically remove them BEFORE the
         * parent row, and flush, or MySQL rejects `delete from exams`
         * with FKrk78bmt53fns7np8casqa3q44 (questions.exam_id -> exams.id).
         * We do NOT rely on @OneToMany(orphanRemoval=true) ordering here.
         */
        exam.getQuestions().clear();
        questionRepository.deleteAll(questionRepository.findByExam_Id(examId));
        questionRepository.flush();

        examRepository.delete(exam);
        examRepository.flush();
    }

    private void requireCanManageExam(
            Authentication authentication,
            Exam exam) {

        User currentUser = currentUser(authentication);

        if (isAdmin(currentUser)) {
            return;
        }

        Course course = exam.getCourse();

        boolean owner = course != null
                && course.getFaculty() != null
                && course.getFaculty().getId() != null
                && course.getFaculty().getId().equals(currentUser.getId());

        if (!owner) {
            throw new AccessDeniedException(
                    "You can only delete exams for your own courses");
        }
    }

    private User currentUser(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserDetails details)) {

            throw new AccessDeniedException("Authentication required");
        }

        return userRepository.findByEmail(details.getUsername())
                .orElseThrow(() -> new AccessDeniedException(
                        "Authenticated user not found"));
    }

    private boolean isAdmin(User user) {
        return user != null
                && user.getRole() != null
                && "ADMIN".equalsIgnoreCase(user.getRole().getName());
    }

    private ExamDTO convertToDTO(Exam exam) {
        return new ExamDTO(
                exam.getId(),
                exam.getCourse().getId(),
                exam.getTitle(),
                exam.getDescription(),
                exam.getTotalQuestions(),
                exam.getTotalMarks(),
                exam.getPassingMarks(),
                exam.getDurationMinutes(),
                exam.getActive(),
                exam.getPublished()
        );
    }
}
