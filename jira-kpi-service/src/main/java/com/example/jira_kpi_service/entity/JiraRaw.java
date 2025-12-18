package com.example.jira_kpi_service.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "jira_raw")
@Getter
@Setter
@NoArgsConstructor
public class JiraRaw {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "issue_key", nullable = false)
    private String issueKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB", nullable = false)
    private JsonNode payload;

    @Column(name = "fetch_type")
    private String fetchType = "GET_ISSUES"; // GET_ISSUES or GET_ISSUE_WORKLOG

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt = Instant.now();

//    @Index(name = "idx_jira_raw_issue_key")
//    @Index(name = "idx_jira_raw_fetched_at")
}
