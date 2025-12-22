package com.example.jira_kpi_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "jira_issue", indexes = {
        @Index(name = "idx_jira_issue_vendor", columnList = "vendor_id"),
        @Index(name = "idx_jira_issue_resolution", columnList = "resolution_date")
})
@Getter
@Setter
@NoArgsConstructor
public class JiraIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "issue_key", nullable = false, unique = true, length = 50)
    private String issueKey;

    @Column(name = "project_key")
    private String projectKey; // eg. PROJ-1234

    private String issuetype;
    private String summary;
    private String description;
    private String priority;
    private String status;
    private String resolution;
    private String assignee;
    private String reporter;
    private String creator;

    @Column(name = "story_points", precision = 10, scale = 2)
    private BigDecimal storyPoints;

    @Column(name = "epic_link")
    private String epicLink;

    @ElementCollection
    @CollectionTable(name = "jira_issue_labels", joinColumns = @JoinColumn(name = "issue_id"))
    @Column(name = "label")
    private Set<String> labels = new HashSet<>();

    private Instant createdAt;
    private Instant updatedAt;
    private Instant resolutionDate;
    private Instant dueDate;

    @Column(name = "lead_time_seconds")
    private Long leadTimeSeconds; // Created → Done

    @Column(name = "cycle_time_seconds")
    private Long cycleTimeSeconds; // First In Progress → Done

    @Column(name = "time_in_review_seconds")
    private Long timeInReviewSeconds;

    @Column(name = "time_in_qa_seconds")
    private Long timeInQaSeconds;

    @Column(name = "time_blocked_seconds")
    private Long timeBlockedSeconds;

    @Column(name = "flow_efficiency_pct", precision = 5, scale = 2)
    private BigDecimal flowEfficiencyPct; // Active / Total time

    @Column(name = "is_reopened")
    private boolean reopened;

    @Column(name = "reopen_count")
    private int reopenCount;

    @Column(name = "fetched_at")
    private Instant fetchedAt = Instant.now();

    @Column(name = "raw_id")
    private Long rawId; // link to jira_raw with raw payload
}
