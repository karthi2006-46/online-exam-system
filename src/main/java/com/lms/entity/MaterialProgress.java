package com.lms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "material_progress",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"student_id", "material_id"}
        )
    }
)
public class MaterialProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

@ManyToOne(optional = false)
@JoinColumn(name = "student_id", nullable = false)
private User student;

@ManyToOne(optional = false)
@JoinColumn(name = "material_id", nullable = false)
private StudyMaterial material;

   @Column(nullable = false)
private Boolean completed = false;
    // PDF tracking
@Column(nullable = false)
private Integer lastPage = 0;

@Column(nullable = false)
private Integer totalPages = 0;
// video tracking
@Column(nullable = false)
private Double watchedSeconds = 0.0;

@Column(nullable = false)
private Double videoDuration = 0.0;

@Column(nullable = false)
private Double lastPlaybackPosition = 0.0;

    private LocalDateTime lastHeartbeatAt;

    // Text tracking
   @Column(nullable = false)
private Boolean textCompleted = false;

    // Completion timestamp
    private LocalDateTime completedAt;

    // Getters and Setters

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

    public StudyMaterial getMaterial() {
        return material;
    }

    public void setMaterial(StudyMaterial material) {
        this.material = material;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public Integer getLastPage() {
        return lastPage;
    }

    public void setLastPage(Integer lastPage) {
        this.lastPage = lastPage;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Double getWatchedSeconds() {
        return watchedSeconds;
    }

    public void setWatchedSeconds(Double watchedSeconds) {
        this.watchedSeconds = watchedSeconds;
    }

    public Double getVideoDuration() {
        return videoDuration;
    }

    public void setVideoDuration(Double videoDuration) {
        this.videoDuration = videoDuration;
    }

    public Double getLastPlaybackPosition() {
        return lastPlaybackPosition;
    }

    public void setLastPlaybackPosition(Double lastPlaybackPosition) {
        this.lastPlaybackPosition = lastPlaybackPosition;
    }

    public LocalDateTime getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(LocalDateTime lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public Boolean getTextCompleted() {
        return textCompleted;
    }

    public void setTextCompleted(Boolean textCompleted) {
        this.textCompleted = textCompleted;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
