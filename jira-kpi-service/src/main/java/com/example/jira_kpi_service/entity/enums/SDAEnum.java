package com.example.jira_kpi_service.entity.enums;

import lombok.Data;
import lombok.Getter;

@Getter
public enum SDAEnum {
    CYBERSURF("Cybersurf"),
    SIDH_SAPIENT("SIDH-Sapient"),
    DRC("DRC"),
    HAPTIK("Haptik"),
    ML_INFO("ML Info"),
    TEKDI("Tekdi"),
    TARENTO("Tarento"),
    TNT("TNT"),
    EY("EY"),
    DELOITTE("Deloitte"),
    PWC("PwC"),
    NSDC("NSDC"),
    GL("GL"),
    DAFFODIL("Daffodil")
    ;

    private final String value;

    SDAEnum(String value) {
        this.value = value;
    }
}