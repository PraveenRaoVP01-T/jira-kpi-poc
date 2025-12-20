package com.example.jira_kpi_service.model;

import com.example.jira_kpi_service.entity.enums.SDAEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupWeekIssueDTO {
    private SDAEnum groupName;
    private int weekOfMonth;
    private long issueCount;
}
