package com.example.jira_kpi_service.service;

import com.example.jira_kpi_service.entity.SyncMetadata;
import com.example.jira_kpi_service.repository.SyncMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class JiraSyncScheduler {

    private final JiraSyncService jiraSyncService;
    private final SyncMetadataRepository metadataRepo;

//    @Scheduled(cron = "${scheduler.incremental-cron}")  // every 30 min
    @Scheduled(cron = "0 * * * * *")
    public void incrementalSync() {
        log.info("Syncing...");
        SyncMetadata syncMetadata = metadataRepo.getLastIncrementalSync();
        Instant last = syncMetadata == null ? null : syncMetadata.getLastIncrementalSyncedAt();
        jiraSyncService.performIncrementalSync(last);
        if(syncMetadata == null) {
            syncMetadata = SyncMetadata.builder()
                    .lastIncrementalSyncedAt(Instant.now())
                    .build();
        } else {
            syncMetadata.setLastIncrementalSyncedAt(Instant.now());
        }
        metadataRepo.save(syncMetadata);
    }

    @Scheduled(cron = "${scheduler.full-sync-cron}")  // weekly full re-sync
    public void weeklyFullSync() {
        jiraSyncService.performFullSync();
        SyncMetadata syncMetadata = metadataRepo.getLastIncrementalSync();
        if(syncMetadata == null) {
            syncMetadata = SyncMetadata.builder()
                    .lastFullSyncedAt(Instant.now())
                    .build();
        } else {
            syncMetadata.setLastFullSyncedAt(Instant.now());
        }

        metadataRepo.save(syncMetadata);
    }
}