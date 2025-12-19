package com.example.jira_kpi_service.controller;

import com.example.jira_kpi_service.model.MonthlyAnalyticsResponse;
import com.example.jira_kpi_service.service.AnalyticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/kpi")
public class KpiController {

    private final AnalyticsService analyticsService;

    public KpiController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/monthly")
    public ResponseEntity<MonthlyAnalyticsResponse> getMonthlyAnalytics(
            @RequestParam(defaultValue = "11") Integer month,
            @RequestParam(defaultValue = "2025") Integer year
    ) {
        return ResponseEntity.ok(analyticsService.getMonthlyAnalytics(year, month));
    }
}
