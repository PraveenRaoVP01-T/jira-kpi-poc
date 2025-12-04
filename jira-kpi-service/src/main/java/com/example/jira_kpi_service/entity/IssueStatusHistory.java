package com.example.jira_kpi_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "issue_status_history", indexes = {
        @Index(name = "idx_status_history_issue", columnList = "issue_id"),
        @Index(name = "idx_status_history_changed", columnList = "changed_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
public class IssueStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id")
    private JiraIssue issue;

    @Column(name = "issue_key", nullable = false)
    private String issueKey;

    @Column(name = "from_status")
    private String fromStatus;

    @Column(name = "to_status")
    private String toStatus;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    private String author;

    // Optional: DEV, REVIEW, QA, BLOCKED, WAITING, DONE
    private String category;
}
