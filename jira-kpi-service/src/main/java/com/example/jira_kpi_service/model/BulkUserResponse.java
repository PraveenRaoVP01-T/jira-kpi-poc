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
public class BulkUserResponse {
    private String self;
    private int maxResults;
    private int startAt;
    private int total;
    private Boolean isLast;

    private List<UserData> values;
}
