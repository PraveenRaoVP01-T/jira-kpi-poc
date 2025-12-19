package com.example.jira_kpi_service.repository;

import com.example.jira_kpi_service.entity.IssueWorklog;
import com.example.jira_kpi_service.model.IssueUserTimeSpentDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface IssueWorklogRepository extends JpaRepository<IssueWorklog, Long> {
    List<IssueWorklog> findByIssueKey(String issueKey);

    @Query("""
        SELECT new com.example.jira_kpi_service.model.IssueUserTimeSpentDTO(
            i.issueKey,
            u.accountId,
            u.displayName,
            u.emailAddress,
            u.groupName,
            i.issuetype,
            i.projectKey,
            SUM(w.timeSpentSeconds)
        )
        FROM IssueWorklog w
        JOIN w.jiraIssue i
        JOIN w.user u
        WHERE i.resolutionDate >= :start
          AND i.resolutionDate < :end
        GROUP BY i.issueKey, u.accountId, u.displayName, u.emailAddress, u.groupName, i.issuetype, i.projectKey
    """)
    List<IssueUserTimeSpentDTO> getTimeSpentPerIssuePerUser(
            Instant start,
            Instant end
    );
}
