package com.example.jira_kpi_service.repository;

import com.example.jira_kpi_service.entity.IssueStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueStatusHistoryRepository extends JpaRepository<IssueStatusHistory, Long> {
    @Modifying
    @Query("DELETE FROM IssueStatusHistory i WHERE i.id = :id")
    void deleteByIssueId(Long id);

    @Query("SELECT ik FROM IssueStatusHistory ik WHERE ik.issueKey = :issueKey ORDER BY ik.changedAt ASC")
    List<IssueStatusHistory> findByIssueKeyOrderByChangedAtAsc(String issueKey);
}
