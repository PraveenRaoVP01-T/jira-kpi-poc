package com.example.jira_kpi_service.repository;

import com.example.jira_kpi_service.entity.IssueWorklog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueWorklogRepository extends JpaRepository<IssueWorklog, Long> {
    List<IssueWorklog> findByIssueKey(String issueKey);
}
