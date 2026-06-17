package com.lms.service;

import com.lms.dto.ExamAttemptDTO;
import com.lms.entity.Exam;
import com.lms.entity.ExamAttempt;
import com.lms.entity.Question;
import com.lms.entity.User;
import com.lms.exception.BadRequestException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.ExamAttemptRepository;
import com.lms.repository.ExamRepository;
import com.lms.repository.QuestionRepository;
import com.lms.repository.UserRepository;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private final Gson gson = new Gson();

    public ExamAttemptDTO startExam(Long examId, Long studentId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found"));

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (!exam.getPublished()) {
            throw new BadRequestException("Exam is not published yet");
        }

        // Check if student already has an in-progress attempt
        if (examAttemptRepository.findByExam_IdAndStudent_IdAndStatusNot(examId, studentId, "SUBMITTED").isPresent()) {
            throw new BadRequestException("You already have an in-progress attempt for this exam");
        }

        ExamAttempt attempt = new ExamAttempt();
        attempt.setExam(exam);
        attempt.setStudent(student);
        attempt.setStartTime(LocalDateTime.now());
        attempt.setEndTime(LocalDateTime.now().plusMinutes(exam.getDurationMinutes()));
        attempt.setStatus("IN_PROGRESS");
        attempt.setAnswers("{}");

        ExamAttempt savedAttempt = examAttemptRepository.save(attempt);
        return convertToDTO(savedAttempt);
    }

    public ExamAttemptDTO getAttemptById(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam attempt not found"));
        return convertToDTO(attempt);
    }

    public List<ExamAttemptDTO> getStudentAttempts(Long studentId) {
        return examAttemptRepository.findByStudent_Id(studentId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ExamAttemptDTO> getExamAttempts(Long examId) {
        return examAttemptRepository.findByExam_Id(examId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ExamAttemptDTO saveAnswers(Long attemptId, String answers) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam attempt not found"));

        if (!attempt.getStatus().equals("IN_PROGRESS")) {
            throw new BadRequestException("Cannot save answers for a submitted exam");
        }

        attempt.setAnswers(answers);
        ExamAttempt updatedAttempt = examAttemptRepository.save(attempt);
        return convertToDTO(updatedAttempt);
    }

    public ExamAttemptDTO submitExam(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam attempt not found"));

        if (!attempt.getStatus().equals("IN_PROGRESS")) {
            throw new BadRequestException("Exam already submitted");
        }

        attempt.setStatus("SUBMITTED");
        attempt.setSubmissionTime(LocalDateTime.now());
        ExamAttempt submittedAttempt = examAttemptRepository.save(attempt);

        // Auto-calculate and save result
        resultService.calculateResult(submittedAttempt);

        return convertToDTO(submittedAttempt);
    }

    public ExamAttemptDTO autoSubmitExam(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam attempt not found"));

        if (!attempt.getStatus().equals("IN_PROGRESS")) {
            throw new BadRequestException("Exam already submitted");
        }

        attempt.setStatus("AUTO_SUBMITTED");
        attempt.setSubmissionTime(LocalDateTime.now());
        ExamAttempt submittedAttempt = examAttemptRepository.save(attempt);

        // Auto-calculate and save result
        resultService.calculateResult(submittedAttempt);

        return convertToDTO(submittedAttempt);
    }

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
