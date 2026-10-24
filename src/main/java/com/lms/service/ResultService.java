package com.lms.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lms.dto.ResultDTO;
import com.lms.entity.ExamAttempt;
import com.lms.entity.Question;
import com.lms.entity.Result;
import com.lms.exception.ResourceNotFoundException;
import com.lms.repository.ExamAttemptRepository;
import com.lms.repository.QuestionRepository;
import com.lms.repository.ResultRepository;
import com.lms.dto.QuestionReviewDTO;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Type;
import java.util.ArrayList;

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

    @Autowired
    private ExamAttemptRepository examAttemptRepository;

    private final Gson gson = new Gson();

    /**
     * Calculates and stores the result after an exam is submitted.
     */
    public void calculateResult(ExamAttempt attempt) {

        List<Question> questions =
                questionRepository.findByExam_Id(
                        attempt.getExam().getId()
                );

        Map<String, String> answers =
                parseAnswers(attempt.getAnswers());

        int obtainedMarks = 0;
        int correctAnswers = 0;
        int wrongAnswers = 0;
        int skippedQuestions = 0;

        for (Question question : questions) {

            String studentAnswer =
                    answers.get(String.valueOf(question.getId()));

            if (studentAnswer == null || studentAnswer.isBlank()) {

                skippedQuestions++;

            } else if (
                    studentAnswer.equalsIgnoreCase(
                            question.getCorrectAnswer()
                    )
            ) {

                correctAnswers++;
                obtainedMarks += question.getMarks();

            } else {

                wrongAnswers++;
            }
        }

        int totalMarks = attempt.getExam().getTotalMarks();

        double percentage = totalMarks > 0
                ? (double) obtainedMarks / totalMarks * 100
                : 0.0;

        String status =
                obtainedMarks >= attempt.getExam().getPassingMarks()
                        ? "PASS"
                        : "FAIL";

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

    /**
     * Retrieves a result by its ID.
     */
    @Transactional(readOnly = true)
    public ResultDTO getResult(Long resultId) {

        Result result = resultRepository.findById(resultId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Result not found"
                        )
                );

        return convertToDTO(result);
    }

    /**
     * Retrieves a result using the exam attempt ID.
     */
    @Transactional(readOnly = true)
    public ResultDTO getResultByAttempt(Long attemptId) {

        Result result =
                resultRepository.findByExamAttempt_Id(attemptId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Result not found"
                                )
                        );

        return convertToDTO(result);
    }

    /**
     * Retrieves all results for a student.
     */
    @Transactional(readOnly = true)
    public List<ResultDTO> getStudentResults(Long studentId) {

        return resultRepository.findByStudent_Id(studentId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all results for an exam.
     */
    @Transactional(readOnly = true)
    public List<ResultDTO> getExamResults(Long examId) {

        return resultRepository.findByExam_Id(examId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all results for a student's course.
     */
    @Transactional(readOnly = true)
    public List<ResultDTO> getCourseResults(
            Long studentId,
            Long courseId
    ) {

        return resultRepository
                .findByStudent_IdAndExam_Course_Id(
                        studentId,
                        courseId
                )
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Returns the score summary and detailed question review.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDetailedResult(Long attemptId) {

        ExamAttempt attempt =
                examAttemptRepository.findById(attemptId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Exam attempt not found"
                                )
                        );

        Result result =
                resultRepository.findByExamAttempt_Id(attemptId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Result not found"
                                )
                        );

        Map<String, String> studentAnswers =
                parseAnswers(attempt.getAnswers());

        List<Question> questions =
                questionRepository.findByExam_Id(
                        attempt.getExam().getId()
                );

        List<Map<String, Object>> questionReviews =
                new ArrayList<>();

        for (Question question : questions) {

            String questionId =
                    String.valueOf(question.getId());

            String studentAnswer =
                    studentAnswers.get(questionId);

            String correctAnswer =
                    question.getCorrectAnswer();

            boolean skipped =
                    studentAnswer == null
                    || studentAnswer.isBlank();

            boolean correct =
                    !skipped
                    && correctAnswer != null
                    && studentAnswer.equalsIgnoreCase(correctAnswer);

            Map<String, Object> review =
                    new HashMap<>();

            review.put("questionId", question.getId());
            review.put("questionNumber", question.getQuestionNumber());
            review.put("questionText", question.getQuestionText());

            review.put("optionA", question.getOptionA());
            review.put("optionB", question.getOptionB());
            review.put("optionC", question.getOptionC());
            review.put("optionD", question.getOptionD());

            review.put("studentAnswer", studentAnswer);
            review.put("correctAnswer", correctAnswer);
            review.put("marks", question.getMarks());

            review.put(
                    "answerStatus",
                    skipped ? "SKIPPED"
                            : correct ? "CORRECT" : "WRONG"
            );

            questionReviews.add(review);
        }

        Map<String, Object> response = new HashMap<>();

        response.put("result", convertToDTO(result));
        response.put("questions", questionReviews);

        return response;
    }

    /**
     * Safely converts the stored JSON answers to a map.
     */
    private Map<String, String> parseAnswers(String answersJson) {

        if (answersJson == null || answersJson.isBlank()) {
            return Collections.emptyMap();
        }

        try {

            Type type =
                    new TypeToken<Map<String, String>>() {}.getType();

            Map<String, String> answers =
                    gson.fromJson(answersJson, type);

            return answers == null
                    ? Collections.emptyMap()
                    : answers;

        } catch (Exception exception) {

            return Collections.emptyMap();
        }
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
            result.getSkippedQuestions(),
            buildQuestionReviews(result.getExamAttempt())
    );
}

private List<QuestionReviewDTO> buildQuestionReviews(
        ExamAttempt attempt) {

    List<Question> questions =
            questionRepository.findByExam_Id(
                    attempt.getExam().getId()
            );

    Type type = new TypeToken<Map<String, String>>() {}.getType();

    Map<String, String> submittedAnswers;

    try {
        submittedAnswers = gson.fromJson(
                attempt.getAnswers(),
                type
        );

        if (submittedAnswers == null) {
            submittedAnswers = new HashMap<>();
        }

    } catch (Exception exception) {
        submittedAnswers = new HashMap<>();
    }

    final Map<String, String> answerMap = submittedAnswers;

    return questions.stream()
            .map(question -> {

                String studentAnswer = answerMap.get(
                        String.valueOf(question.getId())
                );

                String correctAnswer = question.getCorrectAnswer();

                boolean skipped =
                        studentAnswer == null
                        || studentAnswer.isBlank();

                boolean correct =
                        !skipped
                        && correctAnswer != null
                        && studentAnswer.equalsIgnoreCase(correctAnswer);

                return new QuestionReviewDTO(
                        question.getId(),
                        question.getQuestionNumber(),
                        question.getQuestionText(),
                        question.getOptionA(),
                        question.getOptionB(),
                        question.getOptionC(),
                        question.getOptionD(),
                        studentAnswer,
                        correctAnswer,
                        correct,
                        skipped
                );
            })
            .collect(Collectors.toList());
}
}