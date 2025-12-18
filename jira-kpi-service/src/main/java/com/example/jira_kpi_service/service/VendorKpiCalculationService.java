package com.example.jira_kpi_service.service;

import com.example.jira_kpi_service.entity.JiraIssue;
import com.example.jira_kpi_service.entity.VendorKpi;
import com.example.jira_kpi_service.repository.IssueStatusHistoryRepository;
import com.example.jira_kpi_service.repository.JiraIssueRepository;
import com.example.jira_kpi_service.repository.VendorKpiRepository;
import com.example.jira_kpi_service.repository.VendorRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorKpiCalculationService {

    private final VendorRepository vendorRepository;
    private final JiraIssueRepository jiraIssueRepository;
    private final IssueStatusHistoryRepository statusHistoryRepository;
    private final VendorKpiRepository vendorKpiRepository;
    private final ElasticsearchIndexingService esService;

    // Run every day at 4:30 AM → after incremental sync
    @Scheduled(cron = "${scheduler.kpi-calc-cron}")
    @Transactional
    public void calculateDailyKpis() throws IOException {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        calculateForPeriod(yesterday, VendorKpi.PeriodType.daily);
        aggregateWeeklyIfNeeded(yesterday);
        aggregateMonthlyIfNeeded(yesterday);
    }

    public void calculateForPeriod(LocalDate periodDate, VendorKpi.PeriodType periodType) throws IOException {
        log.info("Calculating {} KPIs for {}", periodType, periodDate);

        ZoneId zone = ZoneId.of("UTC");
        Instant start = periodDate.atStartOfDay(zone).toInstant();
        Instant end = switch (periodType) {
            case daily -> start.plus(1, java.time.temporal.ChronoUnit.DAYS);
            case weekly -> start.plus(7, java.time.temporal.ChronoUnit.DAYS);
            case monthly -> start.plus(1, java.time.temporal.ChronoUnit.MONTHS);
            case quarterly -> start.plus(3, java.time.temporal.ChronoUnit.MONTHS);
        };

        List<UUID> vendorIds = vendorRepository.findActiveVendorIds();

        for (UUID vendorId : vendorIds) {
            VendorKpi kpi = new VendorKpi();
            kpi.setVendorId(vendorId);
            kpi.setPeriodDate(periodDate);
            kpi.setPeriodType(periodType);

            List<JiraIssue> completedIssues = jiraIssueRepository.findCompletedInPeriod(vendorId, start, end);
            List<JiraIssue> defects = jiraIssueRepository.findDefectsInPeriod(start, end);

            if (completedIssues.isEmpty()) {
                vendorKpiRepository.upsert(kpi); // zero metrics
//                esService.indexVendorKpi(kpi);
                continue;
            }

            // ────── Delivery KPIs ──────
            kpi.setIssuesCompleted(completedIssues.size());
            kpi.setThroughput(completedIssues.size());

            kpi.setAvgLeadTimeSeconds(avg(completedIssues, JiraIssue::getLeadTimeSeconds));
            kpi.setAvgCycleTimeSeconds(avg(completedIssues, JiraIssue::getCycleTimeSeconds));
            kpi.setAvgTimeInReviewSeconds(avg(completedIssues, JiraIssue::getTimeInReviewSeconds));
            kpi.setAvgTimeInQaSeconds(avg(completedIssues, JiraIssue::getTimeInQaSeconds));
            kpi.setAvgTimeBlockedSeconds(avg(completedIssues, JiraIssue::getTimeBlockedSeconds));

            // ────── Quality KPIs ──────
            long defectCount = defects.stream().filter(d -> d.getVendor().getId().equals(vendorId)).count();
            kpi.setDefectCount((int) defectCount);
            kpi.setDefectDensity(BigDecimal.valueOf(defectCount * 100.0 / completedIssues.size())
                    .setScale(4, RoundingMode.HALF_UP));

            long reopened = completedIssues.stream().filter(JiraIssue::isReopened).count();
            kpi.setReopenRatePct(reopened > 0 ?
                    BigDecimal.valueOf(reopened * 100.0 / completedIssues.size()).setScale(2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO);

            long prodDefects = defects.stream()
                    .filter(d -> d.getLabels().contains("production") || d.getPriority().contains("Blocker"))
                    .count();
            kpi.setProductionDefectCount((int) prodDefects);

            // ────── Predictability KPIs ──────
            BigDecimal planned = completedIssues.stream()
                    .map(JiraIssue::getStoryPoints)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            kpi.setStoryPointsCompleted(planned);
            kpi.setStoryPointsPlanned(planned.multiply(BigDecimal.valueOf(1.2))); // assume 20% buffer
            kpi.setVelocityAccuracyPct(kpi.getStoryPointsPlanned().compareTo(BigDecimal.ZERO) > 0 ?
                    kpi.getStoryPointsCompleted().divide(kpi.getStoryPointsPlanned(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)) : BigDecimal.valueOf(100));

            // Spillover: issues in sprint but not done → future enhancement with sprint data
            kpi.setSpilloverRatePct(BigDecimal.valueOf(15.5)); // placeholder

            // SLA: example — resolved within 7 days of creation
            long slaBreaches = completedIssues.stream()
                    .filter(i -> i.getLeadTimeSeconds() != null && i.getLeadTimeSeconds() > 7 * 24 * 3600)
                    .count();
            kpi.setSlaBreaches((int) slaBreaches);
            kpi.setSlaCompliancePct(!completedIssues.isEmpty() ?
                    BigDecimal.valueOf(100.0 * (completedIssues.size() - slaBreaches) / completedIssues.size())
                            .setScale(2, RoundingMode.HALF_UP) : BigDecimal.valueOf(100));

            // ────── Efficiency KPIs ──────
            kpi.setAvgFlowEfficiencyPct(avgDecimal(completedIssues, i -> i.getFlowEfficiencyPct().doubleValue()));
            kpi.setAvgWip(BigDecimal.valueOf(jiraIssueRepository.countCurrentWipByVendor(vendorId)));

            // Save + index
            vendorKpiRepository.upsert(kpi);
//            esService.indexVendorKpi(kpi);

            log.info("KPI calculated for vendor {} on {}: {} issues, lead time {} days",
                    vendorId, periodDate, kpi.getIssuesCompleted(),
                    kpi.getAvgLeadTimeSeconds() != null ? kpi.getAvgLeadTimeSeconds().divide(BigDecimal.valueOf(86400), 1, RoundingMode.HALF_UP) : "N/A");
        }
    }

    private void aggregateWeeklyIfNeeded(LocalDate dailyDate) throws IOException {
        if (dailyDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            LocalDate weekStart = dailyDate.minusDays(6);
            calculateForPeriod(weekStart, VendorKpi.PeriodType.weekly);
        }
    }

    private void aggregateMonthlyIfNeeded(LocalDate dailyDate) throws IOException {
        if (dailyDate.getDayOfMonth() == dailyDate.lengthOfMonth()) {
            calculateForPeriod(dailyDate.withDayOfMonth(1), VendorKpi.PeriodType.monthly);
        }
    }

    private BigDecimal avg(List<JiraIssue> issues, java.util.function.ToLongFunction<JiraIssue> mapper) {
        return issues.stream()
                .mapToLong(mapper)
                .filter(v -> v > 0)
                .average()
                .stream()
                .mapToObj(BigDecimal::valueOf)
                .map(b -> b.setScale(2, RoundingMode.HALF_UP))
                .findFirst()
                .orElse(null);
    }

    private BigDecimal avgDecimal(List<JiraIssue> issues, java.util.function.ToDoubleFunction<JiraIssue> mapper) {
        return issues.stream()
                .mapToDouble(mapper)
                .filter(v -> v > 0)
                .average()
                .stream()
                .mapToObj(BigDecimal::valueOf)
                .map(b -> b.setScale(2, RoundingMode.HALF_UP))
                .findFirst()
                .orElse(null);
    }

}
