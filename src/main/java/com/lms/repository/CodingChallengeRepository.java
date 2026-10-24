package com.lms.repository;

import com.lms.entity.CodingChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CodingChallengeRepository
        extends JpaRepository<CodingChallenge, Long> {

    List<CodingChallenge> findByCourse_IdAndActiveTrue(Long courseId);

    List<CodingChallenge> findByCourse_Id(Long courseId);

    long countByCourse_IdAndActive(Long courseId, Boolean active);
}