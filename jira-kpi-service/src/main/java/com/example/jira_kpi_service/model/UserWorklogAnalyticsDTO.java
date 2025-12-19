package com.example.jira_kpi_service.model;

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

    private long timeSpentSeconds;
}
