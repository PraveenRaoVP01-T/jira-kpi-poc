package com.example.jira_kpi_service.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class IssueSprintId implements Serializable {
    private String issueKey;
    private Long sprintId;
}
