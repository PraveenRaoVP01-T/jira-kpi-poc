package com.example.jira_kpi_service.model;

import lombok.Data;
import java.util.List;

@Data
public class WeeklyAnalyticsDTO {

    /**
     * Week number within the month (1–5)
     */
    private int weekOfMonth;

    private long issuesResolved;

    private List<IssueAnalyticsDTO> issues;
}
