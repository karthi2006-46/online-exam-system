package com.lms.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "coding_challenge_progress",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"student_id", "challenge_id"}
        )
    }
)
public class CodingChallengeProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // =========================
    // STUDENT
    // =========================

    @ManyToOne
    @JoinColumn(
        name = "student_id",
        nullable = false
    )
    private User student;


    // =========================
    // CODING CHALLENGE
    // =========================

    @ManyToOne
    @JoinColumn(
        name = "challenge_id",
        nullable = false
    )
    private CodingChallenge challenge;


    // =========================
    // COMPLETION
    // =========================

    private Boolean completed = false;


    // =========================
    // SCORE
    // =========================

    private Integer score;


    // =========================
    // GETTERS AND SETTERS
    // =========================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public CodingChallenge getChallenge() {
        return challenge;
    }

    public void setChallenge(CodingChallenge challenge) {
        this.challenge = challenge;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }
}