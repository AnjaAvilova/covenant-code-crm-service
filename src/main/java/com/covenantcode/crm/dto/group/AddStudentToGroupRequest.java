package com.covenantcode.crm.dto.group;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddStudentToGroupRequest {

    @NotNull(message = "ID студента не может быть пустым")
    private Long studentId;
}
