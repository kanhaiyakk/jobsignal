package com.jobsignal.insights.persistence.entity;

import com.jobsignal.insights.model.InsightSummary;
import com.jobsignal.insights.model.SkillCount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "weekly_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklyReportEntity {

    @Id
    private UUID id;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "week_end", nullable = false)
    private LocalDate weekEnd;

    @Column(name = "total_listings", nullable = false)
    private int totalListings;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "top_skills", columnDefinition = "jsonb")
    private List<SkillCount> topSkills;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "seniority_counts", columnDefinition = "jsonb")
    private Map<String, Integer> seniorityCounts;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "remote_policy_counts", columnDefinition = "jsonb")
    private Map<String, Integer> remotePolicyCounts;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "top_companies", columnDefinition = "jsonb")
    private List<String> topCompanies;

    @Column(name = "report_text", columnDefinition = "TEXT")
    private String reportText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static WeeklyReportEntity from(InsightSummary summary, String reportText) {
        var entity = new WeeklyReportEntity();
        entity.id = UUID.randomUUID();
        entity.weekStart = summary.weekStart();
        entity.weekEnd = summary.weekEnd();
        entity.totalListings = summary.totalListings();
        entity.topSkills = summary.topSkills();
        entity.seniorityCounts = summary.seniorityCounts();
        entity.remotePolicyCounts = summary.remotePolicyCounts();
        entity.topCompanies = summary.topCompanies();
        entity.reportText = reportText;
        entity.createdAt = Instant.now();
        entity.updatedAt = Instant.now();
        return entity;
    }
}
