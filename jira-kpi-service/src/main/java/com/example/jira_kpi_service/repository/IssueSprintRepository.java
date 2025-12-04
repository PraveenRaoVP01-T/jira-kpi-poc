package com.example.jira_kpi_service.repository;

import com.example.jira_kpi_service.entity.IssueSprint;
import com.example.jira_kpi_service.entity.IssueSprintId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface IssueSprintRepository extends JpaRepository<IssueSprint, IssueSprintId> {
    List<IssueSprint> findByIssueKey(String issueKey);

    @Query("""
        SELECT iss FROM IssueSprint iss 
        JOIN Sprint s ON iss.sprintId = s.id 
        WHERE s.state = 'CLOSED' 
          AND s.completeDate >= :start 
          AND s.completeDate < :end
        """)
    List<IssueSprint> findIssuesInCompletedSprints(@Param("start") Instant start,
                                                   @Param("end") Instant end);
}
