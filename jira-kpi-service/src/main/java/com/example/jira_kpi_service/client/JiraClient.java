package com.example.jira_kpi_service.client;

import com.example.jira_kpi_service.entity.IssueWorklog;
import com.example.jira_kpi_service.entity.JiraIssue;
import com.example.jira_kpi_service.entity.Users;
import com.example.jira_kpi_service.model.BulkUserResponse;
import com.example.jira_kpi_service.model.UserData;
import com.example.jira_kpi_service.model.WorklogResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JiraClient implements IJiraClient {
    @Autowired
    private WebClient jiraWebClient;

    @Autowired
    private ObjectMapper objectMapper;

//    @Value("")

    private static final int MAX_RESULTS = 5;
    private static final String SEARCH_ENDPOINT = "/rest/api/3/search/jql";
    private static final String ISSUE_ENDPOINT = "/rest/api/3/issue/{issueKey}";
    private static final String ISSUE_WORKLOG_ENDPOINT = "/rest/api/3/issue/{issueKey}/worklog";
    private static final String GET_BULK_USERS = "/rest/api/3/user/bulk";

    // Fields we always need for KPIs
    private static final String FIELDS = String.join(",",
            "key", "summary", "status", "issuetype", "priority", "assignee", "reporter",
            "created", "updated", "resolutiondate", "duedate", "resolution",
            "project", "labels", "storypoints", "epic", "sprint");

    private static final String EXPAND = "changelog,schema,names";

    /**
     * Full incremental sync using JQL with pagination
     */
    public List<JsonNode> searchIssues(String jql, Instant updatedAfter) {
        String formattedUpdatedAfter = null;
        if(updatedAfter != null) {
            formattedUpdatedAfter = toJqlTimeFormat(updatedAfter);
        }
//        else {
//            formattedUpdatedAfter = toJqlTimeFormat(Instant.now().minus(3, ChronoUnit.DAYS));
//        }

        String finalJql = formattedUpdatedAfter != null
                ? "(" + jql + ")"
                : jql;

        log.info("Starting Jira search with JQL: {} & fields: {} & expand: {}", finalJql, FIELDS, EXPAND);

        List<Map<String, Object>> allIssues = new ArrayList<>();
        int startAt = 0;
        int total = Integer.MAX_VALUE;
        String nextPageToken = "";
        boolean isLast = false;

        do {
            SearchResponse response = searchPage(finalJql, startAt, MAX_RESULTS, nextPageToken);
//            total = response.getTotal();
//            startAt += response.getIssues().size();
            if(!response.isLast) {
                nextPageToken = response.getNextPageToken();
            }

            isLast = response.getIsLast();

            response.getIssues().forEach(issue -> {
                Map<String, Object> fullIssue = (Map<String, Object>) issue.get("fields");
                fullIssue.put("key", issue.get("key"));
                if (issue.containsKey("changelog")) {
                    fullIssue.put("changelog", issue.get("changelog"));
                }
                allIssues.add(fullIssue);
            });
            if(!isLast) {
                log.info("Fetched {} issues...", allIssues.size());
                if(allIssues.size() > 100) {
                    isLast = true;
                }
            }
        } while(!isLast);

        log.info("Completed Jira sync: {} issues fetched", allIssues.size());
        JsonNode node =  objectMapper.valueToTree(allIssues);

        List<JsonNode> jsonNode = new ArrayList<>();
        node.forEach(jsonNode::add);
        return jsonNode;
    }

    private String toJqlTimeFormat(Instant updatedAfter) {
        log.info("Updated After: {}", updatedAfter);
        DateTimeFormatter JQL_FORMAT =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return updatedAfter
                .atZone(ZoneId.systemDefault())
                .format(JQL_FORMAT);
    }

    public WorklogResponse getWorklogsForIssueAsEntities(JiraIssue jiraIssue) {
        String url = ISSUE_WORKLOG_ENDPOINT.replace("{issueKey}", jiraIssue.getIssueKey());

        String rawResponse = jiraWebClient.get()
                .uri(url)
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals, res -> Mono.empty())
                .onStatus(HttpStatus.TOO_MANY_REQUESTS::equals, res ->
                        Mono.error(new RuntimeException("Rate limited")))
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                .block(Duration.ofSeconds(30));

        WorklogResponse response = null;

        try {
            response = objectMapper.readValue(rawResponse, WorklogResponse.class);
        } catch (Exception e) {
            log.error("Error while parsing worklog response: {}", e.getMessage());
        }

        if (response == null || response.getWorklogs() == null) {
            return null;
        }

        response.setRawJson(objectMapper.valueToTree(rawResponse));

        return response;
    }

    public List<UserData> getBulkUsers(List<String> accountIds) {
        String url = GET_BULK_USERS + "?accountId=";

        String formulatedAccountIds = getAccountIdsRequestParam(accountIds);
        url +=formulatedAccountIds;

        String rawResponse = jiraWebClient.get()
                .uri(url)
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals, res -> Mono.empty())
                .onStatus(HttpStatus.TOO_MANY_REQUESTS::equals, res ->
                        Mono.error(new RuntimeException("Rate limited")))
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                .block(Duration.ofSeconds(30));

        BulkUserResponse bulkUserResponse = null;

        try {
            bulkUserResponse = objectMapper.readValue(rawResponse, BulkUserResponse.class);
        } catch (Exception e) {
            log.error("Error while parsing bulk user response: {}", e.getMessage());
        }

        if(bulkUserResponse == null || bulkUserResponse.getValues() == null) {
            return List.of();
        }

        return bulkUserResponse.getValues();
    }

    private String getAccountIdsRequestParam(List<String> accountIds) {
        StringBuilder params = new StringBuilder();
        if(accountIds.isEmpty()) {
            return params.toString();
        }
        if(accountIds.size() == 1) {
            return accountIds.getFirst();
        } else {
            for(int i = 0; i < accountIds.size(); i++) {
                if(i == 0) {
                    params.append(accountIds.get(i));
                } else {
                    params.append("&accountId=").append(accountIds.get(i));
                }
            }
        }
        return params.toString();
    }

    private SearchResponse searchPage(String jql, int startAt, int maxResults, String nextPageToken) {

        // 1. Call Jira and get raw JSON as a String
        String rawJson = jiraWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(SEARCH_ENDPOINT)
                        .queryParam("jql", jql)
                        .queryParam("nextPageToken", nextPageToken)
                        .queryParam("startAt", startAt)
                        .queryParam("maxResults", maxResults)
                        .queryParam("fields", FIELDS)
                        .queryParam("expand", EXPAND)
                        .build()
                )
                .retrieve()
                .onStatus(HttpStatus.TOO_MANY_REQUESTS::equals, clientResponse ->
                        Mono.error(new JiraRateLimitException("Rate limited by Jira")))
                .onStatus(HttpStatus.BAD_GATEWAY::equals, clientResponse ->
                        Mono.error(new RuntimeException("Jira temporarily unavailable")))
                .bodyToMono(String.class) // <-- get raw JSON
                .retryWhen(Retry.backoff(5, Duration.ofSeconds(2))
                        .filter(ex -> ex instanceof JiraRateLimitException))
                .block(Duration.ofMinutes(5));

        // 2. Log the raw JSON
        log.info("Jira response JSON: {}", rawJson);

        // 3. Convert the JSON into your SearchResponse POJO
        try {
            return objectMapper.readValue(rawJson, SearchResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse Jira SearchResponse", e);
            throw new RuntimeException("Invalid Jira response", e);
        }
    }



    // Inner response classes
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    private static class SearchResponse {

        String nextPageToken = null;
        List<Map<String, Object>> issues;
        JsonNode names;
        JsonNode schema;

        Boolean isLast;
        @Override
        public String toString() {
            return "Next Page Token: " + nextPageToken + " Issues: " + issues.toString() + " isLast: " + isLast;
        }
    }

    private static class JiraRateLimitException extends RuntimeException {
        public JiraRateLimitException(String message) {
            super(message);
        }
    }
}
