package com.covenantcode.crm.service;

import java.time.LocalDate;
import java.time.LocalTime;

public interface LessonOverlapService {

    void checkTeacherOverlap(Long teacherId,
                             LocalDate lessonDate,
                             LocalTime startTime,
                             LocalTime endTime,
                             Long excludeLessonId);
}
