package com.example.jira_kpi_service.repository;

import com.example.jira_kpi_service.entity.Sprint;
import com.example.jira_kpi_service.entity.SprintId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, SprintId> {
}
