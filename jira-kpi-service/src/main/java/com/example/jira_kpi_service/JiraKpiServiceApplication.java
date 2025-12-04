package com.example.jira_kpi_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JiraKpiServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(JiraKpiServiceApplication.class, args);
	}

}
