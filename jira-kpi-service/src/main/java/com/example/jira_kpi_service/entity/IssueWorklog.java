package com.example.jira_kpi_service.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class IssueWorklog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "jira_issue_id", referencedColumnName = "id")
    private JiraIssue jiraIssue;

    private String issueKey;
    private String timeSpent;
    private Long timeSpentSeconds;
    private OffsetDateTime created;
    private OffsetDateTime updated;
    private OffsetDateTime started;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private JsonNode author;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private JsonNode updateAuthor;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Users user;

}
