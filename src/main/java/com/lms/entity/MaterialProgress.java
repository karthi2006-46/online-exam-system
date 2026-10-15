package com.lms.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "material_progress")
public class MaterialProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="student_id")
    private User student;

    @ManyToOne
    @JoinColumn(name="material_id")
    private StudyMaterial material;

    private Boolean completed = false;

    // Getters Setters
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
}