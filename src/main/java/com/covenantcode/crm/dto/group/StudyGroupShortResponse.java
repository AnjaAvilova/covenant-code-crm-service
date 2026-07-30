package com.covenantcode.crm.dto.group;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudyGroupShortResponse {

    private Long id;
    private String name;
}
