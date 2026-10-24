
package com.lms.service;

import com.lms.dto.ExamAttemptDTO;
import com.lms.entity.Exam;
import com.lms.entity.ExamAttempt;
import com.lms.entity.User;
import com.lms.exception.BadRequestException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.ExamAttemptRepository;
import com.lms.repository.ExamRepository;
import com.lms.repository.QuestionRepository;
import com.lms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExamAttemptService {

    @Autowired
    private ExamAttemptRepository examAttemptRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ResultService resultService;

    // =====================================================
    // START EXAM
    // =====================================================

    public ExamAttemptDTO startExam(Long examId, Long studentId) {

        // Find exam
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Exam not found"));

        // Find student
        User student = userRepository.findById(studentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Student not found"));

        // Check exam publication
        if (!Boolean.TRUE.equals(exam.getPublished())) {
            throw new BadRequestException("Exam is not published yet");
        }

        // Check completed attempts
        List<ExamAttempt> completedAttempts =
                examAttemptRepository
                        .findByExam_IdAndStudent_IdAndStatusIn(
                                examId,
                                studentId,
                                List.of("SUBMITTED", "AUTO_SUBMITTED")
                        );

        if (!completedAttempts.isEmpty()) {
            throw new BadRequestException(
                    "You have already completed this exam. Please view your result."
            );
        }

        // Resume an existing attempt
        List<ExamAttempt> inProgressAttempts =
                examAttemptRepository
                        .findByExam_IdAndStudent_IdAndStatus(
                                examId,
                                studentId,
                                "IN_PROGRESS"
                        );

        if (!inProgressAttempts.isEmpty()) {
            return convertToDTO(inProgressAttempts.get(0));
        }

        // Create a new attempt
        ExamAttempt attempt = new ExamAttempt();

        attempt.setExam(exam);
        attempt.setStudent(student);

        LocalDateTime now = LocalDateTime.now();

        attempt.setStartTime(now);
        attempt.setEndTime(
                now.plusMinutes(exam.getDurationMinutes())
        );

        attempt.setStatus("IN_PROGRESS");
        attempt.setAnswers("{}");

        ExamAttempt savedAttempt =
                examAttemptRepository.save(attempt);

        return convertToDTO(savedAttempt);
    }

    // =====================================================
    // GET STUDENT ATTEMPT FOR A PARTICULAR EXAM
    // =====================================================

    @Transactional(readOnly = true)
    public Optional<ExamAttemptDTO> getStudentExamAttempt(
            Long examId,
            Long studentId
    ) {

        List<ExamAttempt> attempts =
                examAttemptRepository.findByStudent_IdAndExam_Id(
                        studentId,
                        examId
                );

        if (attempts.isEmpty()) {
            return Optional.empty();
        }

        // Return a completed attempt first
        for (ExamAttempt attempt : attempts) {

            if ("SUBMITTED".equals(attempt.getStatus())
                    || "AUTO_SUBMITTED".equals(attempt.getStatus())) {

                return Optional.of(convertToDTO(attempt));
            }
        }

        // Otherwise return an attempt in progress
        for (ExamAttempt attempt : attempts) {

            if ("IN_PROGRESS".equals(attempt.getStatus())) {
                return Optional.of(convertToDTO(attempt));
            }
        }

        // No matching active or completed attempt
        return Optional.empty();
    }

    // =====================================================
    // GET ATTEMPT BY ID
    // =====================================================

    @Transactional(readOnly = true)
    public ExamAttemptDTO getAttemptById(Long attemptId) {

        ExamAttempt attempt =
                examAttemptRepository.findById(attemptId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Exam attempt not found"
                                ));

        return convertToDTO(attempt);
    }

    // =====================================================
    // GET ALL ATTEMPTS FOR A STUDENT
    // =====================================================

    @Transactional(readOnly = true)
    public List<ExamAttemptDTO> getStudentAttempts(Long studentId) {

        return examAttemptRepository.findByStudent_Id(studentId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // =====================================================
    // GET ALL ATTEMPTS FOR AN EXAM
    // =====================================================

    @Transactional(readOnly = true)
    public List<ExamAttemptDTO> getExamAttempts(Long examId) {

        return examAttemptRepository.findByExam_Id(examId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // =====================================================
    // SAVE ANSWERS
    // =====================================================

    public ExamAttemptDTO saveAnswers(
            Long attemptId,
            String answers
    ) {

        ExamAttempt attempt =
                examAttemptRepository.findById(attemptId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Exam attempt not found"
                                ));

        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            throw new BadRequestException(
                    "Cannot save answers for a submitted exam"
            );
        }

        attempt.setAnswers(answers);

        ExamAttempt updatedAttempt =
                examAttemptRepository.save(attempt);

        return convertToDTO(updatedAttempt);
    }

    // =====================================================
    // SUBMIT EXAM
    // =====================================================

    public ExamAttemptDTO submitExam(Long attemptId) {

        ExamAttempt attempt =
                examAttemptRepository.findById(attemptId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Exam attempt not found"
                                ));

        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            throw new BadRequestException("Exam already submitted");
        }

        attempt.setStatus("SUBMITTED");
        attempt.setSubmissionTime(LocalDateTime.now());

        ExamAttempt submittedAttempt =
                examAttemptRepository.save(attempt);

        // Calculate and save result
        resultService.calculateResult(submittedAttempt);

        return convertToDTO(submittedAttempt);
    }

    // =====================================================
    // AUTO-SUBMIT EXAM
    // =====================================================

    public ExamAttemptDTO autoSubmitExam(Long attemptId) {

        ExamAttempt attempt =
                examAttemptRepository.findById(attemptId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Exam attempt not found"
                                ));

        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            throw new BadRequestException("Exam already submitted");
        }

        attempt.setStatus("AUTO_SUBMITTED");
        attempt.setSubmissionTime(LocalDateTime.now());

        ExamAttempt submittedAttempt =
                examAttemptRepository.save(attempt);

        // Calculate and save result
        resultService.calculateResult(submittedAttempt);

        return convertToDTO(submittedAttempt);
    }


    // =====================================================
    // ENTITY TO DTO
    // =====================================================

    private ExamAttemptDTO convertToDTO(ExamAttempt attempt) {

        return new ExamAttemptDTO(
                attempt.getId(),
                attempt.getExam().getId(),
                attempt.getStudent().getId(),
                attempt.getStatus(),
                attempt.getAnswers()
        );
    }
}