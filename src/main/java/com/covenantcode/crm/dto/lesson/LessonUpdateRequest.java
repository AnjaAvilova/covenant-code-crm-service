package com.covenantcode.crm.dto.lesson;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class LessonUpdateRequest {
    @NotNull
    private Long groupId;

    @NotNull
    private Long teacherId;

    @NotBlank
    @Size(max = 255)
    private String topic;

    @Size(max = 1000)
    private String description;

    @NotNull
    private LocalDate lessonDate;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;
}
