package com.example.jira_kpi_service.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;

public interface IJiraClient {
    List<JsonNode> searchIssues(String jql, Instant updatedAfter);
}
