package com.lms.repository;

import com.lms.entity.ReportMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportMetadataRepository
        extends JpaRepository<ReportMetadata, Long> {

    Optional<ReportMetadata> findByReportId(String reportId);
}