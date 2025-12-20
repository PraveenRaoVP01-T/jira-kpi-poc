package com.example.jira_kpi_service.model;

import com.example.jira_kpi_service.entity.enums.SDAEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MonthlyAnalyticsResponse {
    private int year;
    private int month;

    private SDAEnum jiraSda;

    private List<GroupAnalyticsDTO> groups;

    private List<WeeklyAnalyticsDTO> weeklyBreakdown;

}
