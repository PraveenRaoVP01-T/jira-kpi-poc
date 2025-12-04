package com.example.jira_kpi_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "sprint")
@Getter
@Setter
@NoArgsConstructor
@IdClass(SprintId.class)
public class Sprint {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "rapid_view_id")
    private Long rapidViewId;

    @Column(nullable = false)
    private String name;

    private String state;
    private Instant startDate;
    private Instant endDate;
    private Instant completeDate;
    private String goal;

    @Column(name = "fetched_at")
    private Instant fetchedAt = Instant.now();
}

