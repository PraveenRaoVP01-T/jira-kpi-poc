package com.example.jira_kpi_service.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.example.jira_kpi_service.entity.VendorKpi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ElasticsearchIndexingService {

    private final ElasticsearchClient esClient;
    private static final String INDEX_PATTERN = "vendor-kpi-";

    public void indexVendorKpi(VendorKpi kpi) throws IOException {
        String indexName = INDEX_PATTERN + kpi.getPeriodDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd"));

        Map<String, Object> doc = new HashMap<>();
        doc.put("vendor_id", kpi.getVendorId());
        doc.put("vendor_name", kpi.getVendor().getName());
        doc.put("period_date", kpi.getPeriodDate());
        doc.put("period_type", kpi.getPeriodType().name());
        doc.put("issues_completed", kpi.getIssuesCompleted());
        doc.put("avg_lead_time_days", kpi.getAvgLeadTimeSeconds() != null ? kpi.getAvgLeadTimeSeconds().doubleValue() / 86400 : null);
        doc.put("avg_cycle_time_days", kpi.getAvgCycleTimeSeconds() != null ? kpi.getAvgCycleTimeSeconds().doubleValue() / 86400 : null);
        doc.put("defect_density", kpi.getDefectDensity());
        doc.put("reopen_rate_pct", kpi.getReopenRatePct());
        doc.put("flow_efficiency_pct", kpi.getAvgFlowEfficiencyPct());
        doc.put("sla_compliance_pct", kpi.getSlaCompliancePct());
        doc.put("computed_at", Instant.now());

        esClient.index(i -> i
                .index(indexName)
                .id(kpi.getVendorId() + "_" + kpi.getPeriodDate() + "_" + kpi.getPeriodType())
                .document(doc)
        );
    }
}
