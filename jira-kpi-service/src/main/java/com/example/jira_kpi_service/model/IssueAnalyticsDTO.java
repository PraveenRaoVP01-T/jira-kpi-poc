package com.example.jira_kpi_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IssueAnalyticsDTO {
    private String issueKey;

    private String issueType;
    private String projectKey;
    private long totalTimeSpentSeconds;

    private List<UserWorklogAnalyticsDTO> users;
}
