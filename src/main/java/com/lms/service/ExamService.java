package com.lms.service;

import com.lms.dto.ExamDTO;
import com.lms.entity.Course;
import com.lms.entity.Exam;
import com.lms.exception.BadRequestException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.CourseRepository;
import com.lms.repository.ExamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExamService {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private CourseRepository courseRepository;

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
        return examRepository.findByCourse_Id(courseId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ExamDTO> getPublishedExams(Long courseId) {
        return examRepository.findByCourse_IdAndPublishedTrue(courseId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ExamDTO publishExam(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found"));

        if (exam.getQuestions().isEmpty()) {
            throw new BadRequestException("Cannot publish exam without questions");
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

    public void deleteExam(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found"));
        exam.setActive(false);
        examRepository.save(exam);
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
