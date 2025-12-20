package com.example.jira_kpi_service.repository;

import com.example.jira_kpi_service.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long> {
    List<Users> findByJiraAccountIdIn(Set<String> uniqueAccountIds);
}
