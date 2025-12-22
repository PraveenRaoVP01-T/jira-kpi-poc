package com.example.jira_kpi_service.entity;

import com.example.jira_kpi_service.entity.enums.ProjectNameEnum;
import com.example.jira_kpi_service.entity.enums.SDAEnum;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "jira_account_id", unique = true, nullable = false)
    private String jiraAccountId;
    @Column(name = "jira_display_name", unique = true, nullable = false)
    private String jiraDisplayName;
    private String jiraEmailAddress;
    private boolean isActiveInJira;

    private String jiraProjectName = "SIDH";
    
    private String emailDomainName; // to segregate reports SDA-wise

    @Enumerated(EnumType.STRING)
    private ProjectNameEnum assignedProjectName; // (assigned manually) one SDA has multiple projects like NAPS, ITI, etc. This will help us to filter as per project assigned to SDA

    @Enumerated(EnumType.STRING)
    private SDAEnum jiraSDA; // assigned manually in master table based on email domain
}
