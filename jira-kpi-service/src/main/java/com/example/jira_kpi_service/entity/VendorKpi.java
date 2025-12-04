package com.example.jira_kpi_service.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "vendor_kpi", indexes = {
        @Index(name = "idx_vendor_kpi_vendor_period", columnList = "vendor_id, period_type, period_date DESC"),
        @Index(name = "idx_vendor_kpi_period_type", columnList = "period_type, period_date DESC")
})
@Getter
@Setter
@NoArgsConstructor
public class VendorKpi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "vendor_id", insertable = false, updatable = false)
    private UUID vendorId;

    @Column(name = "period_date", nullable = false)
    private LocalDate periodDate;

    @Column(name = "period_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PeriodType periodType;

    // Delivery
    @Column(name = "issues_completed")
    private Integer issuesCompleted = 0;

    private Integer throughput = 0;

    @Column(name = "avg_lead_time_seconds", precision = 12, scale = 2)
    private BigDecimal avgLeadTimeSeconds;

    @Column(name = "avg_cycle_time_seconds", precision = 12, scale = 2)
    private BigDecimal avgCycleTimeSeconds;

    @Column(name = "avg_time_in_review_seconds", precision = 12, scale = 2)
    private BigDecimal avgTimeInReviewSeconds;

    @Column(name = "avg_time_in_qa_seconds", precision = 12, scale = 2)
    private BigDecimal avgTimeInQaSeconds;

    @Column(name = "avg_time_blocked_seconds", precision = 12, scale = 2)
    private BigDecimal avgTimeBlockedSeconds;

    // Quality
    @Column(name = "defect_count")
    private Integer defectCount = 0;

    @Column(name = "defect_density", precision = 8, scale = 4)
    private BigDecimal defectDensity;

    @Column(name = "reopen_rate_pct", precision = 6, scale = 2)
    private BigDecimal reopenRatePct;

    @Column(name = "production_defect_count")
    private Integer productionDefectCount = 0;

    // Predictability
    @Column(name = "story_points_planned", precision = 10, scale = 2)
    private BigDecimal storyPointsPlanned;

    @Column(name = "story_points_completed", precision = 10, scale = 2)
    private BigDecimal storyPointsCompleted;

    @Column(name = "velocity_accuracy_pct", precision = 6, scale = 2)
    private BigDecimal velocityAccuracyPct;

    @Column(name = "spillover_rate_pct", precision = 6, scale = 2)
    private BigDecimal spilloverRatePct;

    @Column(name = "sla_compliance_pct", precision = 6, scale = 2)
    private BigDecimal slaCompliancePct;

    @Column(name = "sla_breaches")
    private Integer slaBreaches = 0;

    // Efficiency
    @Column(name = "avg_flow_efficiency_pct", precision = 6, scale = 2)
    private BigDecimal avgFlowEfficiencyPct;

    @Column(name = "avg_wip", precision = 8, scale = 2)
    private BigDecimal avgWip;

    @Column(name = "computed_at")
    private Instant computedAt = Instant.now();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private JsonNode meta;

    public enum PeriodType { daily, weekly, monthly, quarterly }
}
