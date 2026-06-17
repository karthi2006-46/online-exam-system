package com.lms.service;

import com.lms.dto.ResultDTO;
import com.lms.entity.ExamAttempt;
import com.lms.entity.Question;
import com.lms.entity.Result;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.QuestionRepository;
import com.lms.repository.ResultRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class ResultService {

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private QuestionRepository questionRepository;

    private final Gson gson = new Gson();

    public void calculateResult(ExamAttempt attempt) {
        List<Question> questions = questionRepository.findByExam_Id(attempt.getExam().getId());
        Type type = new TypeToken<Map<String, String>>(){}.getType();
        Map<String, String> answers = gson.fromJson(attempt.getAnswers(), type);

        int obtainedMarks = 0;
        int correctAnswers = 0;
        int wrongAnswers = 0;
        int skippedQuestions = questions.size() - answers.size();

        for (Question question : questions) {
            String studentAnswer = answers.get(String.valueOf(question.getId()));
            if (studentAnswer != null && studentAnswer.equals(question.getCorrectAnswer())) {
                correctAnswers++;
                obtainedMarks += question.getMarks();
            } else if (studentAnswer != null) {
                wrongAnswers++;
            }
        }

        double percentage = (double) obtainedMarks / attempt.getExam().getTotalMarks() * 100;
        String status = obtainedMarks >= attempt.getExam().getPassingMarks() ? "PASS" : "FAIL";

        Result result = new Result();
        result.setExamAttempt(attempt);
        result.setExam(attempt.getExam());
        result.setStudent(attempt.getStudent());
        result.setObtainedMarks(obtainedMarks);
        result.setPercentage(percentage);
        result.setStatus(status);
        result.setCorrectAnswers(correctAnswers);
        result.setWrongAnswers(wrongAnswers);
        result.setSkippedQuestions(skippedQuestions);

        resultRepository.save(result);
    }

    public ResultDTO getResult(Long resultId) {
        Result result = resultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException("Result not found"));
        return convertToDTO(result);
    }

    public ResultDTO getResultByAttempt(Long attemptId) {
        Result result = resultRepository.findByExamAttempt_Id(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Result not found"));
        return convertToDTO(result);
    }

    public List<ResultDTO> getStudentResults(Long studentId) {
        return resultRepository.findByStudent_Id(studentId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ResultDTO> getExamResults(Long examId) {
        return resultRepository.findByExam_Id(examId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ResultDTO> getCourseResults(Long studentId, Long courseId) {
        return resultRepository.findByStudent_IdAndExam_Course_Id(studentId, courseId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ResultDTO convertToDTO(Result result) {
        return new ResultDTO(
                result.getId(),
                result.getExam().getId(),
                result.getExam().getTitle(),
                result.getObtainedMarks(),
                result.getExam().getTotalMarks(),
                result.getPercentage(),
                result.getStatus(),
                result.getCorrectAnswers(),
                result.getWrongAnswers(),
                result.getSkippedQuestions()
        );
    }
}
