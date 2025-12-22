package com.example.jira_kpi_service.repository;

import com.example.jira_kpi_service.entity.JiraIssue;
import com.example.jira_kpi_service.entity.enums.ProjectNameEnum;
import com.example.jira_kpi_service.entity.enums.SDAEnum;
import com.example.jira_kpi_service.model.GroupWeekIssueDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JiraIssueRepository extends JpaRepository<JiraIssue, Long> {
    Optional<JiraIssue> findByIssueKey(String issueKey);



    @Query("""
        SELECT i FROM JiraIssue i 
        WHERE i.issuetype IN ('Bug', 'Production Bug') 
          AND i.resolutionDate >= :start 
          AND i.resolutionDate < :end
        """)
    List<JiraIssue> findDefectsInPeriod(@Param("start") Instant start,
                                        @Param("end") Instant end);

    @Query("SELECT DISTINCT(i.issuetype) FROM JiraIssue i")
    List<String> findUniqueIssueTypes();


    @Query("""
        SELECT new com.example.jira_kpi_service.model.GroupWeekIssueDTO(
            u.jiraSDA,
            (CAST(EXTRACT(DAY FROM i.resolutionDate) AS int) - 1) / 7 + 1,
            COUNT(DISTINCT i.id),
            u.assignedProjectName
        )
        FROM JiraIssue i
        JOIN IssueWorklog w ON w.jiraIssue = i
        JOIN Users u ON w.user = u
        WHERE i.resolutionDate >= :start
          AND i.resolutionDate < :end
        GROUP BY u.jiraSDA, (CAST(EXTRACT(DAY FROM i.resolutionDate) AS int) - 1) / 7 + 1, u.assignedProjectName
    """)
    List<GroupWeekIssueDTO> getIssueCountsPerGroupPerWeek(
            Instant start,
            Instant end
    );

    @Query("""
    SELECT new com.example.jira_kpi_service.model.GroupWeekIssueDTO(
        u.jiraSDA,
        (CAST(EXTRACT(DAY FROM i.resolutionDate) AS int) - 1) / 7 + 1,
        COUNT(DISTINCT i.id),
        u.assignedProjectName
    )
    FROM JiraIssue i
    JOIN IssueWorklog w ON w.jiraIssue = i
    JOIN Users u ON w.user = u
    WHERE u.jiraSDA = :jiraSda
      AND (:assignedProjectName IS NULL OR u.assignedProjectName = :assignedProjectName)
      AND i.resolutionDate >= :start
      AND i.resolutionDate < :end
      AND (:issueType IS NULL OR i.issuetype = :issueType)
    GROUP BY u.jiraSDA, u.assignedProjectName, (CAST(EXTRACT(DAY FROM i.resolutionDate) AS int) - 1) / 7 + 1
""")
    List<GroupWeekIssueDTO> getWeeklyIssueCountsForSda(
            SDAEnum jiraSda,
            Instant start,
            Instant end,
            String issueType,
            ProjectNameEnum assignedProjectName
    );


    @Query("SELECT COUNT(*) FROM JiraIssue ji WHERE ji.resolutionDate >= :start AND ji.resolutionDate < :end")
    int totalIssuesResolved(Instant start, Instant end);

}
