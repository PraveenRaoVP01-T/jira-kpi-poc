package com.example.jira_kpi_service.repository;

import com.example.jira_kpi_service.entity.SyncMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface SyncMetadataRepository extends JpaRepository<SyncMetadata, Long> {
    @Query("SELECT s FROM SyncMetadata s LIMIT 1")
    SyncMetadata getLastIncrementalSync();

}
