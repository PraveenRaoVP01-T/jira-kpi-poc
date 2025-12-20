package com.example.jira_kpi_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class JiraConfig {
    @Value("${jira.base-url}")
    private String baseUrl;

    @Value("${jira.username}")
    private String username;

    @Value("${jira.token}")
    private String token;

    @Value("${jira.project-keys:}")
    private String projectKeys; // comma-separated

    @Value("${jira.vendor-custom-field:customfield_10010}")
    private String vendorCustomFieldId;

    @Bean
    public WebClient jiraWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Authorization", "Basic " + java.util.Base64.getEncoder()
                        .encodeToString((username + ":" + token).getBytes()))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(config -> config
                                .defaultCodecs()
                                .maxInMemorySize(10 * 1024 * 1024))
                        .build())
                .build();
    }

    // Helper beans for services
    @Bean
    public String[] jiraProjectKeysArray() {
        return projectKeys.isBlank() ? new String[0] : projectKeys.split("\\s*,\\s*");
    }
}
