package com.example.jira_kpi_service.service;

import com.example.jira_kpi_service.entity.enums.SDAEnum;
import com.example.jira_kpi_service.model.*;
import com.example.jira_kpi_service.repository.IssueWorklogRepository;
import com.example.jira_kpi_service.repository.JiraIssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {
    private final JiraIssueRepository jiraIssueRepository;
    private final IssueWorklogRepository issueWorklogRepository;

    public MonthlyAnalyticsResponse getMonthlyAnalytics(int year, int month) {

        Instant monthStart = getMonthStart(year, month);
        Instant monthEnd = getMonthEnd(year, month);

        List<GroupWeekIssueDTO> issueCounts =
                jiraIssueRepository.getIssueCountsPerGroupPerWeek(monthStart, monthEnd);

        List<IssueUserTimeSpentDTO> userEfforts =
                issueWorklogRepository.getTimeSpentPerIssuePerUser(monthStart, monthEnd);

        return buildResponse(year, month, issueCounts, userEfforts);
    }

    private WeeklyAnalyticsDTO findOrCreateWeek(
            List<WeeklyAnalyticsDTO> weeks,
            int weekOfMonth) {

        return weeks.stream()
                .filter(w -> w.getWeekOfMonth() == weekOfMonth)
                .findFirst()
                .orElseGet(() -> {
                    WeeklyAnalyticsDTO w = new WeeklyAnalyticsDTO();
                    w.setWeekOfMonth(weekOfMonth);
                    w.setIssues(new ArrayList<>());
                    weeks.add(w);
                    return w;
                });
    }

    private Instant getMonthStart(int year, int month) {
        return LocalDate.of(year, month, 1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
    }

    private Instant getMonthEnd(int year, int month) {
        return LocalDate.of(year, month, 1)
                .plusMonths(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
    }

    private MonthlyAnalyticsResponse buildResponse(
            int year,
            int month,
            List<GroupWeekIssueDTO> issueCounts,
            List<IssueUserTimeSpentDTO> userEfforts) {

        MonthlyAnalyticsResponse response = new MonthlyAnalyticsResponse();
        response.setYear(year);
        response.setMonth(month);

        Map<String, GroupAnalyticsDTO> groupMap = new HashMap<>();

        // 1️⃣ Populate issue counts
        for (GroupWeekIssueDTO dto : issueCounts) {

            if(dto.getGroupName() == null) continue;
            GroupAnalyticsDTO group =
                    groupMap.computeIfAbsent(dto.getGroupName().getValue(), g -> {
                        GroupAnalyticsDTO ga = new GroupAnalyticsDTO();
                        ga.setGroupName(g);
                        ga.setWeeklyBreakdown(new ArrayList<>());
                        return ga;
                    });

            group.setTotalIssuesResolved(
                    group.getTotalIssuesResolved() + dto.getIssueCount()
            );

            WeeklyAnalyticsDTO weekly = findOrCreateWeek(
                    group.getWeeklyBreakdown(), dto.getWeekOfMonth()
            );

            weekly.setIssuesResolved(dto.getIssueCount());
        }

        // 2️⃣ Populate user effort per issue
        Map<String, IssueAnalyticsDTO> issueMap = new HashMap<>();

        for (IssueUserTimeSpentDTO dto : userEfforts) {

            IssueAnalyticsDTO issue =
                    issueMap.computeIfAbsent(dto.getIssueKey(), key -> {
                        IssueAnalyticsDTO ia = new IssueAnalyticsDTO();
                        ia.setIssueKey(key);
                        ia.setIssueType(dto.getIssueType());
                        ia.setProjectKey(dto.getProjectKey());
                        ia.setUsers(new ArrayList<>());
                        return ia;
                    });

            UserWorklogAnalyticsDTO user = new UserWorklogAnalyticsDTO();
            user.setAccountId(dto.getAccountId());
            user.setDisplayName(dto.getDisplayName());
            user.setEmailAddress(dto.getEmailAddress());
            user.setGroupName(dto.getGroupName() == null ? "NSDC" : dto.getGroupName().getValue());
            user.setTimeSpentSeconds(dto.getTimeSpentSeconds());

            issue.getUsers().add(user);
            issue.setTotalTimeSpentSeconds(
                    issue.getTotalTimeSpentSeconds() + dto.getTimeSpentSeconds()
            );
        }

        // 3️⃣ Attach issues to weekly buckets by group
        for (GroupAnalyticsDTO group : groupMap.values()) {
            for (WeeklyAnalyticsDTO week : group.getWeeklyBreakdown()) {
                week.setIssues(
                        issueMap.values().stream().toList()
                );
            }
        }

        response.setGroups(new ArrayList<>(groupMap.values()));
        return response;
    }

    public List<String> getIssueTypes() {
        return jiraIssueRepository.findUniqueIssueTypes();
    }

    public MonthlyAnalyticsResponse getSdaAnalytics(SDAEnum jiraSda, Integer year, Integer month, String issueType) {
        int resolvedYear = (year != null) ? year : 2025;
        int resolvedMonth = (month != null) ? month : 11;

        Instant start = getMonthStart(resolvedYear, resolvedMonth);
        Instant end = getMonthEnd(resolvedYear, resolvedMonth);

        List<GroupWeekIssueDTO> weeklyCounts =
                jiraIssueRepository.getWeeklyIssueCountsForSda(
                        jiraSda, start, end, issueType);

        List<IssueUserTimeSpentDTO> worklogs =
                issueWorklogRepository.getSdaIssueWorklogs(
                        jiraSda, start, end, issueType);

        return buildSdaResponse(
                jiraSda,
                resolvedYear,
                resolvedMonth,
                weeklyCounts,
                worklogs
        );
    }

    private MonthlyAnalyticsResponse buildSdaResponse(SDAEnum jiraSda, int resolvedYear, int resolvedMonth, List<GroupWeekIssueDTO> weeklyCounts, List<IssueUserTimeSpentDTO> worklogs) {
        MonthlyAnalyticsResponse response = new MonthlyAnalyticsResponse();
        response.setJiraSda(jiraSda);
        response.setYear(resolvedYear);
        response.setMonth(resolvedMonth);

        Map<Integer, WeeklyAnalyticsDTO> weekMap = new HashMap<>();
        Map<String, IssueAnalyticsDTO> issueMap = new HashMap<>();

        /* -----------------------------
         * 1️⃣ Weekly issue counts
         * ----------------------------- */
        for (GroupWeekIssueDTO dto : weeklyCounts) {

            WeeklyAnalyticsDTO week =
                    weekMap.computeIfAbsent(dto.getWeekOfMonth(), w -> {
                        WeeklyAnalyticsDTO wd = new WeeklyAnalyticsDTO();
                        wd.setWeekOfMonth(w);
                        wd.setIssues(new ArrayList<>());
                        wd.setIssuesResolved(0);
                        return wd;
                    });

            week.setIssuesResolved(
                    week.getIssuesResolved() + dto.getIssueCount()
            );
        }

        /* -----------------------------
         * 2️⃣ Issue + user worklogs
         * ----------------------------- */
        for (IssueUserTimeSpentDTO dto : worklogs) {

            IssueAnalyticsDTO issue =
                    issueMap.computeIfAbsent(dto.getIssueKey(), key -> {
                        IssueAnalyticsDTO ia = new IssueAnalyticsDTO();
                        ia.setIssueKey(key);
                        ia.setIssueType(dto.getIssueType());
                        ia.setProjectKey(dto.getProjectKey());
                        ia.setUsers(new ArrayList<>());
                        ia.setTotalTimeSpentSeconds(0);
                        return ia;
                    });

            UserWorklogAnalyticsDTO user = new UserWorklogAnalyticsDTO();
            user.setAccountId(dto.getAccountId());
            user.setDisplayName(dto.getDisplayName());
            user.setEmailAddress(dto.getEmailAddress());
            user.setGroupName(dto.getGroupName().getValue());
            user.setTimeSpentSeconds(dto.getTimeSpentSeconds());

            issue.getUsers().add(user);
            issue.setTotalTimeSpentSeconds(
                    issue.getTotalTimeSpentSeconds() + dto.getTimeSpentSeconds()
            );
        }

        /* -----------------------------
         * 3️⃣ Attach issues to weeks
         * ----------------------------- */
        // NOTE:
        // Issues appear in weeks where SDA work happened
        // (accurate for effort analytics)

        for (WeeklyAnalyticsDTO week : weekMap.values()) {
            week.setIssues(
                    issueMap.values().stream().toList()
            );
        }

        response.setWeeklyBreakdown(
                weekMap.values().stream()
                        .sorted(Comparator.comparingInt(WeeklyAnalyticsDTO::getWeekOfMonth))
                        .toList()
        );

        return response;
    }
}
