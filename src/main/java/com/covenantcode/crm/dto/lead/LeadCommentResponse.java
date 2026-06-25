package com.covenantcode.crm.dto.lead;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ответ с данными о комментарии")
public class LeadCommentResponse {

    @Schema(description = "Идентификатор комментария", example = "1")
    Long id;

    @Schema(description = "Идентификатор лида, к которому относится комментарий", example = "2")
    Long leadId;

    @Schema(description = "Автор комментария (id, firstName, lastName)", example = "1")
    UserShortResponse author;

    @Schema(description = "Текст комментария", example = "1")
    String text;

    @Schema(description = "Дата и время создания", example = "2026-06-29T10:00")
    LocalDateTime createdAt;
}
