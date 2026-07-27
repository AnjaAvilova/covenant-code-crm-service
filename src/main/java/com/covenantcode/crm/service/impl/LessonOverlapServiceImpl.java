package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.entity.Lesson;
import com.covenantcode.crm.exception.ConflictException;
import com.covenantcode.crm.repository.LessonRepository;
import com.covenantcode.crm.service.LessonOverlapService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonOverlapServiceImpl implements LessonOverlapService {

    private final LessonRepository lessonRepository;

    @Override
    @Transactional(readOnly = true)
    public void checkTeacherOverlap(Long teacherId,
                                    LocalDate lessonDate,
                                    LocalTime startTime,
                                    LocalTime endTime,
                                    Long excludeLessonId) {

        List<Lesson> existingLessons = lessonRepository.findByTeacherIdAndLessonDate(teacherId, lessonDate);

        for (Lesson lesson : existingLessons) {

            if (excludeLessonId != null && lesson.getId().equals(excludeLessonId)) {
                continue;
            }

            if (startTime.isBefore(lesson.getEndTime()) && lesson.getStartTime().isBefore(endTime)) {
                throw new ConflictException(
                        String.format("Преподаватель уже занят в это время. Конфликтующее занятие: %s %s-%s",
                                lessonDate,
                                lesson.getStartTime(),
                                lesson.getEndTime())
                );
            }
        }
    }
}