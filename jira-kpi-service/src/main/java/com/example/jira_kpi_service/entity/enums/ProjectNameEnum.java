package com.example.jira_kpi_service.entity.enums;

import lombok.Getter;

@Getter
public enum ProjectNameEnum {
    SIDH("SIDH"),
    TNT_SIDH("TNT-SIDH"),
    TNT_NAPS("TNT-NAPS"),
    TNT_PMV("TNT-PMV"),
    TNT_ITI("TNT-ITI"),
    HEPTIK_SIDH("Heptik-SIDH"),
    TARENTO_SIDH("Tarento-SIDH"),
    TEKDI_SIDH("Tekdi-SIDH"),
    CYBERSURF_SIDH("Cybersurf-SIDH"),
    MLINFO_SIDH("ML Info-SIDH"),
    DRC_SIDH("DRC-SIDH");

    private final String value;


    ProjectNameEnum(String value) {
        this.value = value;
    }
}
