package com.example.jira_kpi_service.entity.enums;

import lombok.Data;
import lombok.Getter;

@Getter
public enum SDAEnum {
    CYBERSURF("Cybersurf"),
    SIDH_SAPIENT("SIDH-Sapient"), // n
    DRC("DRC"),
    HAPTIK("Haptik"),
    ML_INFO("ML Info"),
    TEKDI("Tekdi"),
    TARENTO("Tarento"), // n
    TNT("TNT"),
    EY("EY"), // n
    DELOITTE("Deloitte"), // n
    PWC("PwC"), // n
    NSDC("NSDC"), // n
    GL("GL"), // n
    DAFFODIL("Daffodil") // n
    ;

    private final String value;

    SDAEnum(String value) {
        this.value = value;
    }
}