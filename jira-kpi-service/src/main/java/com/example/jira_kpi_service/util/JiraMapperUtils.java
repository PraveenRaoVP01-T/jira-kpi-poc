package com.example.jira_kpi_service.util;

import com.example.jira_kpi_service.entity.IssueWorklog;
import com.example.jira_kpi_service.entity.JiraIssue;
import com.example.jira_kpi_service.entity.Users;
import com.example.jira_kpi_service.model.DomainExtractDTO;
import com.example.jira_kpi_service.model.UserData;
import com.example.jira_kpi_service.model.WorklogResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
@Slf4j
public class JiraMapperUtils {


    public static IssueWorklog mapToIssueWorklog(WorklogResponse.Worklogs wl, JiraIssue jiraIssue, Users user) {
        ObjectMapper objectMapper = new ObjectMapper();
        return IssueWorklog.builder()
                .issueKey(jiraIssue.getIssueKey())
                .timeSpent(wl.getTimeSpent())
                .jiraIssue(jiraIssue)
                .timeSpentSeconds(wl.getTimeSpentSeconds())
                .created(parseDateTime(wl.getCreated()))
                .updated(parseDateTime(wl.getUpdated()))
                .started(parseDateTime(wl.getStarted()))
                .author(objectMapper.valueToTree(wl.getAuthor()))
                .updateAuthor(objectMapper.valueToTree(wl.getUpdateAuthor()))
                .user(user)
                .build();
    }

    public static OffsetDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return null;
        }

        try {
            DateTimeFormatter OFFSET_FORMATTER =
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            return OffsetDateTime.parse(dateTimeStr, OFFSET_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse Jira timestamp: '{}'. Falling back to LocalDateTime.", dateTimeStr);
            return LocalDateTime.parse(dateTimeStr,
                            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"))
                    .atOffset(ZoneOffset.UTC);
        }
    }

    public static Users mapToUserEntity(UserData userData) {
        ObjectMapper objectMapper = new ObjectMapper();

        DomainExtractDTO domainDetails = getDomainDetailsFromEmail(userData.getEmailAddress());
        return Users.builder()
                .jiraAccountId(userData.getAccountId())
                .jiraDisplayName(userData.getDisplayName())
                .jiraEmailAddress(userData.getEmailAddress())
                .isActiveInJira(userData.isActive())
                .emailDomainName(domainDetails.getDomainName())
                .jiraAvatarUrls(objectMapper.valueToTree(userData.getAvatarUrls()))
                .build();
    }

    private static DomainExtractDTO getDomainDetailsFromEmail(String email) {
        if(email == null) {
            return new DomainExtractDTO("nsdc.org", "nsdc");
        }

        String domain = email.substring(email.indexOf('@') + 1);
        String groupName = domain.substring(0, domain.indexOf('.'));

        return new DomainExtractDTO(domain, groupName);

    }

    public static String formatDuration(long seconds) {
        if (seconds <= 0) return "0s";

        long days = seconds / 86400;
        seconds %= 86400;

        long hours = seconds / 3600;
        seconds %= 3600;

        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder result = new StringBuilder();

        if (days > 0) result.append(days).append("d ");
        if (hours > 0) result.append(hours).append("h ");
        if (minutes > 0) result.append(minutes).append("m ");
        if (seconds > 0) result.append(seconds).append("s ");

        return result.toString().trim();
    }
}
