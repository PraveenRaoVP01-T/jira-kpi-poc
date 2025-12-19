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
public class GroupAnalyticsDTO {
    private String groupName;

    private long totalIssuesResolved;

    private List<WeeklyAnalyticsDTO> weeklyBreakdown;
}
