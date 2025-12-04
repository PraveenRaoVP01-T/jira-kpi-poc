package com.example.jira_kpi_service.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "jira.sync.initial-delay=1s",
        "jira.sync.fixed-delay=1h"
})
@Slf4j
@DirtiesContext
class FullPipelineTest {

    @Autowired
    JiraSyncService syncService;

    @Test
    void shouldLoadFakeDataAndCalculateKPIs() throws Exception {
        log.info("Starting fake sync...");
        syncService.performFullSync();  // Manual trigger

        Thread.sleep(8000); // Wait for processing

        // Now check DB has data
        // Or just watch logs: you'll see "Processed 2 issues", "KPI calculated..."
    }
}