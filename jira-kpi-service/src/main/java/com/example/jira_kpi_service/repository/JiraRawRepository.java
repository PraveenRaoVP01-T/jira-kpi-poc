package com.example.jira_kpi_service.repository;

import com.example.jira_kpi_service.entity.JiraRaw;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JiraRawRepository extends JpaRepository<JiraRaw, Long> {
}
