package com.covenantcode.crm.dto.lesson;
import com.covenantcode.crm.dto.group.StudyGroupShortResponse;
import com.covenantcode.crm.dto.group.UserShortResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Data
@Builder
public class LessonResponse {

    private Long id;
    private StudyGroupShortResponse studyGroup;
    private UserShortResponse teacher;
    private String topic;
    private String description;
    private LocalDate lessonDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
