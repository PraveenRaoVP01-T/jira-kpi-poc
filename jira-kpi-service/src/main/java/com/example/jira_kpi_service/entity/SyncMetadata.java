package com.example.jira_kpi_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Data
@Entity
@Table(name = "sync_metadata")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SyncMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Instant lastFullSyncedAt;
    private Instant lastIncrementalSyncedAt;
}
