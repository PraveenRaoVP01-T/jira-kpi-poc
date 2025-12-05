package com.example.jira_kpi_service.service;

import com.example.jira_kpi_service.client.JiraClient;
import com.example.jira_kpi_service.entity.IssueStatusHistory;
import com.example.jira_kpi_service.entity.JiraIssue;
import com.example.jira_kpi_service.entity.JiraRaw;
import com.example.jira_kpi_service.entity.Vendor;
import com.example.jira_kpi_service.repository.IssueStatusHistoryRepository;
import com.example.jira_kpi_service.repository.JiraIssueRepository;
import com.example.jira_kpi_service.repository.JiraRawRepository;
import com.example.jira_kpi_service.repository.VendorRepository;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class JiraSyncService {

    private final JiraClient jiraClient;
    private final JiraRawRepository jiraRawRepository;
    private final JiraIssueRepository jiraIssueRepository;
    private final IssueStatusHistoryRepository statusHistoryRepository;
    private final VendorRepository vendorRepository;

    @Value("${jira.vendor-custom-field}")
    private final String vendorCustomFieldId;

    @Value("${jira.project-keys:PROJ1,PROJ2}")
    private String jiraProjectKeys;


    @Transactional
    public void performFullSync() {
        log.info("Starting FULL Jira sync...");
        syncInternal(null);
    }

    @Transactional
    public void performIncrementalSync(Instant since) {
        log.info("Starting INCREMENTAL Jira sync since {}...", since);
        syncInternal(since);
    }

    private void syncInternal(Instant updatedAfter) {
//        String BASE_JQL = """
//        project IN (%s)
//        AND issuetype IN (Story, Task, Bug, Sub-task, Epic)
//        AND statusCategory IN (Done, "In Progress")
//        """.formatted(String.join(",",  jiraProjectKeys));
        String BASE_JQL = """
                project IN (%s)
                """.formatted(String.join(",", jiraProjectKeys));

        List<JsonNode> rawIssues = jiraClient.searchIssues(BASE_JQL, LocalDateTime.of(LocalDate.of(2025,12,3), LocalTime.MIDNIGHT).toInstant(ZoneOffset.UTC));
        log.info("Fetched data....");
        var counter = new AtomicInteger(0);

        rawIssues.forEach(rawIssue -> {
            try {
                String issueKey = rawIssue.get("key").asText();
                JsonNode fields = rawIssue;

                // 1. Save raw payload (immutable audit trail)
                JiraRaw jiraRaw = saveRawPayload(issueKey, rawIssue);

                // 2. Normalize + upsert issue
                JiraIssue jiraIssue = normalizeIssue(issueKey, fields, jiraRaw.getId());
                jiraIssue = jiraIssueRepository.save(jiraIssue);

                // 3. Extract and save status history + compute times
                extractAndSaveStatusHistory(jiraIssue, fields.path("changelog"));

                if (counter.incrementAndGet() % 100 == 0) {
                    log.info("Processed {} issues...", counter.get());
                }
            } catch (Exception e) {
                log.error("Failed to process issue from raw JSON", e);
            }
        });

        log.info("Jira sync completed: {} issues processed", counter.get());
    }

    private JiraRaw saveRawPayload(String issueKey, com.fasterxml.jackson.databind.JsonNode payload) {
        JiraRaw raw = new JiraRaw();
        raw.setIssueKey(issueKey);
        raw.setPayload(payload);
        raw.setFetchedAt(Instant.now());
        return jiraRawRepository.save(raw);
    }

    private JiraIssue normalizeIssue(String issueKey, JsonNode fields, Long rawId) {
        JiraIssue issue = jiraIssueRepository.findByIssueKey(issueKey)
                .orElse(new JiraIssue());

        issue.setIssueKey(issueKey);
        issue.setRawId(rawId);
        issue.setProjectKey(fields.path("project").path("key").asText());
        issue.setIssuetype(fields.path("issuetype").path("name").asText());
        issue.setSummary(fields.path("summary").asText());
        issue.setStatus(fields.path("status").path("name").asText());
        issue.setPriority(fields.path("priority").path("name").asText(null));
        issue.setAssignee(displayName(fields.path("assignee")));
        issue.setReporter(displayName(fields.path("reporter")));
        issue.setCreator(displayName(fields.path("creator")));

        issue.setCreatedAt(toInstant(fields.path("created")));
        issue.setUpdatedAt(toInstant(fields.path("updated")));
        issue.setResolutionDate(toInstant(fields.path("resolutiondate")));
        issue.setDueDate(toInstant(fields.path("duedate")));

        // Story points
        JsonNode sp = fields.path("customfield_10016"); // common story point field
        if (sp.isMissingNode() || sp.isNull()) sp = fields.path("storypoints");
        if (!sp.isMissingNode() && sp.isNumber()) {
            issue.setStoryPoints(BigDecimal.valueOf(sp.asDouble()));
        }

        // Vendor extraction
        JsonNode vendorNode = fields.path(vendorCustomFieldId);
        String vendorName = vendorNode.isTextual() ? vendorNode.asText() :
                vendorNode.path("value").asText(null);

        Vendor vendor = vendorName != null ?
                vendorRepository.findByNameIgnoreCase(vendorName)
                        .orElseGet(() -> createOrGetVendor(vendorName)) : null;

        issue.setVendor(vendor);

        // Labels
        Set<String> labels = new HashSet<>();
        fields.path("labels").forEach(l -> labels.add(l.asText()));
        issue.setLabels(labels);

        // Materialized metrics (recomputed every time — cheap & accurate)
        if (issue.getResolutionDate() != null) {
            computeMaterializedMetrics(issue);
        }

        return issue;
    }

    private void extractAndSaveStatusHistory(JiraIssue jiraIssue, JsonNode changelog) {
        // Delete old history (idempotent sync)
        statusHistoryRepository.deleteByIssueId(jiraIssue.getId());

        List<IssueStatusHistory> history = new ArrayList<>();

        changelog.path("histories").forEach(hist -> {
            String author = displayName(hist.path("author"));
            Instant created = toInstant(hist.path("created"));

            hist.path("items").forEach(item -> {
                if ("status".equals(item.path("field").asText())) {
                    String from = item.path("fromString").asText();
                    String to = item.path("toString").asText();

                    IssueStatusHistory h = new IssueStatusHistory();
                    h.setIssue(jiraIssue);
                    h.setIssueKey(jiraIssue.getIssueKey());
                    h.setFromStatus(from);
                    h.setToStatus(to);
                    h.setChangedAt(created);
                    h.setAuthor(author);
                    h.setCategory(categorizeStatus(to));
                    history.add(h);
                }
            });
        });

        statusHistoryRepository.saveAll(history);
    }

    private void computeMaterializedMetrics(JiraIssue issue) {
        Instant created = issue.getCreatedAt();
        Instant resolved = issue.getResolutionDate();

        // Lead Time
        issue.setLeadTimeSeconds(created != null && resolved != null ?
                ChronoUnit.SECONDS.between(created, resolved) : null);

        // Cycle Time: first "In Progress" → Done
        List<IssueStatusHistory> hist = statusHistoryRepository
                .findByIssueKeyOrderByChangedAtAsc(issue.getIssueKey());

        Instant firstInProgress = null;
        Instant doneTime = null;
        long reviewSeconds = 0, qaSeconds = 0, blockedSeconds = 0, activeSeconds = 0;
        Instant lastTime = issue.getCreatedAt();

        for (IssueStatusHistory h : hist) {
            if (firstInProgress == null && isInProgress(h.getToStatus())) {
                firstInProgress = h.getChangedAt();
            }
            if (isDone(h.getToStatus())) {
                doneTime = h.getChangedAt();
            }

            long duration = ChronoUnit.SECONDS.between(lastTime, h.getChangedAt());
            String cat = h.getCategory();
            if ("DEV".equals(cat) || "REVIEW".equals(cat) || "QA".equals(cat)) {
                activeSeconds += duration;
            } else if ("BLOCKED".equals(cat)) {
                blockedSeconds += duration;
            }
            if ("REVIEW".equals(cat)) reviewSeconds += duration;
            if ("QA".equals(cat)) qaSeconds += duration;

            lastTime = h.getChangedAt();
        }

        if (firstInProgress != null && doneTime != null) {
            issue.setCycleTimeSeconds(ChronoUnit.SECONDS.between(firstInProgress, doneTime));
        }

        issue.setTimeInReviewSeconds(reviewSeconds);
        issue.setTimeInQaSeconds(qaSeconds);
        issue.setTimeBlockedSeconds(blockedSeconds);

        long totalTime = ChronoUnit.SECONDS.between(created, resolved);
        issue.setFlowEfficiencyPct(totalTime > 0 ?
                BigDecimal.valueOf(activeSeconds * 100.0 / totalTime)
                        .setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        // Reopen detection
        boolean reopened = hist.stream()
                .anyMatch(h -> isDone(h.getFromStatus()) && !isDone(h.getToStatus()));
        issue.setReopened(reopened);
        issue.setReopenCount((int) hist.stream()
                .filter(h -> isDone(h.getFromStatus()) && !isDone(h.getToStatus()))
                .count());
    }

    // Helper methods
    private String displayName(JsonNode node) {
        return node.isMissingNode() ? null : node.path("displayName").asText(null);
    }

    private Instant toInstant(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) return null;
        return Instant.parse(node.asText());
    }

    private Vendor createOrGetVendor(String name) {
        return vendorRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    Vendor v = new Vendor();
                    v.setName(name);
                    v.setShortCode(name.substring(0, Math.min(10, name.length())).toUpperCase());
                    v.setActive(true);
                    return vendorRepository.save(v);
                });
    }

    private String categorizeStatus(String status) {
        String s = status.toLowerCase();
        if (s.contains("review") || s.contains("code review")) return "REVIEW";
        if (s.contains("qa") || s.contains("test")) return "QA";
        if (s.contains("block") || s.contains("wait")) return "BLOCKED";
        if (s.contains("progress") || s.contains("dev")) return "DEV";
        if (s.contains("done") || s.contains("closed")) return "DONE";
        return "WAITING";
    }

    private boolean isInProgress(String status) {
        return status != null && status.toLowerCase().contains("progress");
    }

    private boolean isDone(String status) {
        return status != null && (status.toLowerCase().contains("done") || status.toLowerCase().contains("closed"));
    }
}
