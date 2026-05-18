package com.jobsignal.insights.persistence.repository;

import com.jobsignal.insights.persistence.entity.WeeklyReportEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReportEntity, UUID> {

    Page<WeeklyReportEntity> findAllByOrderByWeekEndDesc(Pageable pageable);

    Optional<WeeklyReportEntity> findTopByOrderByWeekEndDesc();
}
