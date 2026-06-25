package com.covenantcode.crm.dto.lead;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class LeadCommentCreateRequest {

    @NotBlank(message = "Комментарий не может быть пустым")
    @Size(max =1000, message = "Длинна комментария не может превышать 1000 символов")
    private String text;
}
