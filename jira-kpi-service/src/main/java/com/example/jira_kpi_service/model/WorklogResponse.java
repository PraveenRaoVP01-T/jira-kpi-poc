package com.example.jira_kpi_service.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WorklogResponse {
    private Long startAt;
    private Long maxResults;
    private Long total;
    private List<Worklogs> worklogs;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Worklogs {
        private String self;
        private Map<String, Object> author;
        private Map<String, Object> updateAuthor;
        private String created;
        private String updated;
        private String started;
        private String timeSpent;
        private Long timeSpentSeconds;
        private String id;
        private String issueId;
        private Map<String, Object> comment;
        private Map<String, Object> visibility;
    }

    private JsonNode rawJson;
}
