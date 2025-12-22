package com.example.jira_kpi_service.model;

import com.example.jira_kpi_service.entity.enums.ProjectNameEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserWorklogAnalyticsDTO {
    private String accountId;
    private String displayName;
    private String emailAddress;

    private String groupName;
    private ProjectNameEnum assignedProjectName;

    private long timeSpentSeconds;
    private String timeSpent;
}
