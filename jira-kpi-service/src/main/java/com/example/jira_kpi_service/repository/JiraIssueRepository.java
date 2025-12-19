package com.example.jira_kpi_service.repository;

import com.example.jira_kpi_service.entity.JiraIssue;
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
        WHERE i.vendor.id = :vendorId 
          AND i.resolutionDate >= :start 
          AND i.resolutionDate < :end
        """)
    List<JiraIssue> findCompletedInPeriod(@Param("vendorId") UUID vendorId,
                                          @Param("start") Instant start,
                                          @Param("end") Instant end);

    @Query("""
        SELECT i FROM JiraIssue i 
        WHERE i.issuetype IN ('Bug', 'Production Bug') 
          AND i.resolutionDate >= :start 
          AND i.resolutionDate < :end
        """)
    List<JiraIssue> findDefectsInPeriod(@Param("start") Instant start,
                                        @Param("end") Instant end);

    @Query("SELECT COUNT(i) FROM JiraIssue i WHERE i.vendor.id = :vendorId AND i.status NOT IN ('Done', 'Closed')")
    Long countCurrentWipByVendor(@Param("vendorId") UUID vendorId);





    @Query("""
        SELECT new com.example.jira_kpi_service.model.GroupWeekIssueDTO(
            u.groupName,
            (CAST(EXTRACT(DAY FROM i.resolutionDate) AS int) - 1) / 7 + 1,
            COUNT(DISTINCT i.id)
        )
        FROM JiraIssue i
        JOIN IssueWorklog w ON w.jiraIssue = i
        JOIN Users u ON w.user = u
        WHERE i.resolutionDate >= :start
          AND i.resolutionDate < :end
        GROUP BY u.groupName, (CAST(EXTRACT(DAY FROM i.resolutionDate) AS int) - 1) / 7 + 1
    """)
    List<GroupWeekIssueDTO> getIssueCountsPerGroupPerWeek(
            Instant start,
            Instant end
    );



}
