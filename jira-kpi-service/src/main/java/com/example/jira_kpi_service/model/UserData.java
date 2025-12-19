package com.example.jira_kpi_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserData {
    private String self;
    private String accountId;
    private String accountType;
    private String emailAddress = null;
    private Map<String, Object> avatarUrls;
    private String displayName;
    private boolean isActive;
    private String timeZone;
}
