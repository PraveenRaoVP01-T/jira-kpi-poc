package com.example.jira_kpi_service.service;

import com.example.jira_kpi_service.client.IJiraClient;
import com.example.jira_kpi_service.client.JiraClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
@Profile("test")
@RequiredArgsConstructor
@Slf4j
public class FakeJiraClient implements IJiraClient {

    @Override
    public List<com.fasterxml.jackson.databind.JsonNode> searchIssues(String jql, Instant updatedAfter) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode array = mapper.readTree(
                    new File("src/test/resources/sample-jira-data.json"));
            log.info("Loading files to db");
            return StreamSupport.stream(array.spliterator(), false)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
