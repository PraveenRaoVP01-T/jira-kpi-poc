package com.example.jira_kpi_service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JiraClient implements IJiraClient {
    @Autowired
    private WebClient jiraWebClient;

    @Autowired
    private ObjectMapper objectMapper;

    private static final int MAX_RESULTS = 100;
    private static final String SEARCH_ENDPOINT = "/rest/api/3/search";
    private static final String ISSUE_ENDPOINT = "/rest/api/3/issue/{issueKey}";

    // Fields we always need for KPIs
    private static final String FIELDS = String.join(",",
            "key", "summary", "status", "issuetype", "priority", "assignee", "reporter",
            "created", "updated", "resolutiondate", "duedate", "resolution",
            "customfield_10010", // replace with actual vendor field if different
            "project", "labels", "storypoints", "epic", "sprint");

    private static final String EXPAND = "changelog,schema,names";

    /**
     * Full incremental sync using JQL with pagination
     */
    public List<JsonNode> searchIssues(String jql, Instant updatedAfter) {
        String finalJql = updatedAfter != null
                ? "(" + jql + ") AND updated >= \"" + updatedAfter.atZone(java.time.ZoneOffset.UTC).toLocalDate() + "\""
                : jql;

        log.info("Starting Jira search with JQL: {}", finalJql);

        List<Map<String, Object>> allIssues = new ArrayList<>();
        int startAt = 0;
        int total = Integer.MAX_VALUE;

        while (startAt < total) {
            SearchResponse response = searchPage(finalJql, startAt, MAX_RESULTS);
            total = response.getTotal();
            startAt += response.getIssues().size();

            response.getIssues().forEach(issue -> {
                Map<String, Object> fullIssue = (Map<String, Object>) issue.get("fields");
                fullIssue.put("key", issue.get("key"));
                if (issue.containsKey("changelog")) {
                    fullIssue.put("changelog", issue.get("changelog"));
                }
                allIssues.add(fullIssue);
            });

            log.info("Fetched {}/{} issues...", allIssues.size(), total);
        }

        log.info("Completed Jira sync: {} issues fetched", allIssues.size());
        return objectMapper.valueToTree(allIssues);
    }

    private SearchResponse searchPage(String jql, int startAt, int maxResults) {
        return jiraWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(SEARCH_ENDPOINT)
                        .queryParam("jql", jql)
                        .queryParam("startAt", startAt)
                        .queryParam("maxResults", maxResults)
                        .queryParam("fields", FIELDS)
                        .queryParam("expand", EXPAND)
                        .build())
                .retrieve()
                .onStatus(HttpStatus.TOO_MANY_REQUESTS::equals, clientResponse ->
                        Mono.error(new JiraRateLimitException("Rate limited by Jira")))
                .onStatus(HttpStatus.BAD_GATEWAY::equals, clientResponse ->
                        Mono.error(new RuntimeException("Jira temporarily unavailable")))
                .bodyToMono(SearchResponse.class)
                .retryWhen(Retry.backoff(5, Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof JiraRateLimitException))
                .block(Duration.ofMinutes(5));
    }

    // Inner response classes
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    private static class SearchResponse {

        private int startAt;
        int maxResults;
        int total;
        List<Map<String, Object>> issues;
    }

    private static class JiraRateLimitException extends RuntimeException {
        public JiraRateLimitException(String message) {
            super(message);
        }
    }
}
