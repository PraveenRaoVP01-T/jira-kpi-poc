package com.example.jira_kpi_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "issue_sprint")
@IdClass(IssueSprintId.class)
@Getter
@Setter
@NoArgsConstructor
public class IssueSprint {

    @Id
    @Column(name = "issue_key")
    private String issueKey;

    @Id
    @Column(name = "sprint_id")
    private Long sprintId;

    @Column(name = "added_at")
    private Instant addedAt = Instant.now();

    @Column(name = "removed_at")
    private Instant removedAt;
}

