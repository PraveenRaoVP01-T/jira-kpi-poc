package com.example.jira_kpi_service.controller;

import com.example.jira_kpi_service.entity.enums.ProjectNameEnum;
import com.example.jira_kpi_service.entity.enums.SDAEnum;
import com.example.jira_kpi_service.model.MonthlyAnalyticsResponse;
import com.example.jira_kpi_service.service.AnalyticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping("/get/issue/types")
    public ResponseEntity<List<String>> getIssueTypes() {
        return ResponseEntity.ok(analyticsService.getIssueTypes());
    }

    @GetMapping("/sda")
    public MonthlyAnalyticsResponse getAnalytics(
            @RequestParam SDAEnum jiraSda,
            @RequestParam(required = false) ProjectNameEnum assignedProjectName,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String issueType) {

        return analyticsService.getSdaAnalytics(
                jiraSda, year, month, issueType, assignedProjectName);
    }
}
