package com.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionReviewDTO {

    private Long questionId;

    private Integer questionNumber;

    private String questionText;

    private String optionA;

    private String optionB;

    private String optionC;

    private String optionD;

    private String studentAnswer;

    private String correctAnswer;

    private boolean correct;

    private boolean skipped;

}