package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.entity.Lesson;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.exception.ConflictException;
import com.covenantcode.crm.repository.LessonRepository;
import com.covenantcode.crm.service.LessonOverlapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonOverlapServiceImplTest {

    @Mock
    private LessonRepository lessonRepository;

    private LessonOverlapService lessonOverlapService;

    private static final Long TEACHER_ID = 1L;
    private static final LocalDate LESSON_DATE = LocalDate.of(2026, 5, 20);

    @BeforeEach
    void setUp() {
        lessonOverlapService = new LessonOverlapServiceImpl(lessonRepository);
    }

    // Тест 1: нет занятий в этот день — исключение не выбрасывается
    @Test
    void shouldNotThrowExceptionWhenNoLessonsExist() {
        // given
        when(lessonRepository.findByTeacherIdAndLessonDate(TEACHER_ID, LESSON_DATE))
                .thenReturn(List.of());

        // when/then
        assertThatCode(() -> lessonOverlapService.checkTeacherOverlap(
                TEACHER_ID,
                LESSON_DATE,
                LocalTime.of(10, 0),
                LocalTime.of(11, 30),
                null
        )).doesNotThrowAnyException();
    }

    // Тест 2: занятие примыкает вплотную (граничный случай) — конфликта нет
    @Test
    void shouldNotThrowExceptionWhenLessonAdjacent() {
        // given: Существующее занятие: 09:00–10:00
        Lesson existingLesson = createLesson(1L, LocalTime.of(9, 0), LocalTime.of(10, 0));
        when(lessonRepository.findByTeacherIdAndLessonDate(TEACHER_ID, LESSON_DATE))
                .thenReturn(List.of(existingLesson));

        // when/then: Новое занятие: 10:00–11:00
        assertThatCode(() -> lessonOverlapService.checkTeacherOverlap(
                TEACHER_ID,
                LESSON_DATE,
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                null
        )).doesNotThrowAnyException();
    }

    // Тест 3: занятия пересекаются — выбрасывается ConflictException
    @Test
    void shouldThrowConflictExceptionWhenOverlapping() {
        // given: Существующее занятие: 09:00–11:00
        Lesson existingLesson = createLesson(1L, LocalTime.of(9, 0), LocalTime.of(11, 0));
        when(lessonRepository.findByTeacherIdAndLessonDate(TEACHER_ID, LESSON_DATE))
                .thenReturn(List.of(existingLesson));

        // when/then: Новое занятие: 10:00–12:00
        assertThatThrownBy(() -> lessonOverlapService.checkTeacherOverlap(
                TEACHER_ID,
                LESSON_DATE,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                null
        )).isInstanceOf(ConflictException.class)
                .hasMessageContaining("Преподаватель уже занят в это время");
    }

    // Тест 4: частичное перекрытие в начале — выбрасывается ConflictException
    @Test
    void shouldThrowConflictExceptionWhenPartialOverlapAtStart() {
        // given: Существующее занятие: 10:00–12:00
        Lesson existingLesson = createLesson(1L, LocalTime.of(10, 0), LocalTime.of(12, 0));
        when(lessonRepository.findByTeacherIdAndLessonDate(TEACHER_ID, LESSON_DATE))
                .thenReturn(List.of(existingLesson));

        // when/then: Новое занятие: 09:00–10:30
        assertThatThrownBy(() -> lessonOverlapService.checkTeacherOverlap(
                TEACHER_ID,
                LESSON_DATE,
                LocalTime.of(9, 0),
                LocalTime.of(10, 30),
                null
        )).isInstanceOf(ConflictException.class)
                .hasMessageContaining("Преподаватель уже занят в это время");
    }

    // Тест 5: одно занятие полностью внутри другого — выбрасывается ConflictException
    @Test
    void shouldThrowConflictExceptionWhenLessonFullyInside() {
        // given: Существующее занятие: 09:00–12:00
        Lesson existingLesson = createLesson(1L, LocalTime.of(9, 0), LocalTime.of(12, 0));
        when(lessonRepository.findByTeacherIdAndLessonDate(TEACHER_ID, LESSON_DATE))
                .thenReturn(List.of(existingLesson));

        // when/then: Новое занятие: 10:00–11:00
        assertThatThrownBy(() -> lessonOverlapService.checkTeacherOverlap(
                TEACHER_ID,
                LESSON_DATE,
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                null
        )).isInstanceOf(ConflictException.class)
                .hasMessageContaining("Преподаватель уже занят в это время");
    }

    // Тест 6: excludeLessonId исключает занятие из проверки — конфликта нет
    @Test
    void shouldNotThrowExceptionWhenExcludingOwnLesson() {
        // given: Существующее занятие с ID=5: 09:00–11:00
        Long excludeLessonId = 5L;
        Lesson existingLesson = createLesson(excludeLessonId, LocalTime.of(9, 0), LocalTime.of(11, 0));
        when(lessonRepository.findByTeacherIdAndLessonDate(TEACHER_ID, LESSON_DATE))
                .thenReturn(List.of(existingLesson));

        // when/then: Новое время для этого же занятия: 09:00–11:00, excludeLessonId = 5
        assertThatCode(() -> lessonOverlapService.checkTeacherOverlap(
                TEACHER_ID,
                LESSON_DATE,
                LocalTime.of(9, 0),
                LocalTime.of(11, 0),
                excludeLessonId
        )).doesNotThrowAnyException();
    }

    // Дополнительный тест: перекрытие в конце
    @Test
    void shouldThrowConflictExceptionWhenPartialOverlapAtEnd() {
        // given: Существующее занятие: 09:00–11:00
        Lesson existingLesson = createLesson(1L, LocalTime.of(9, 0), LocalTime.of(11, 0));
        when(lessonRepository.findByTeacherIdAndLessonDate(TEACHER_ID, LESSON_DATE))
                .thenReturn(List.of(existingLesson));

        // when/then: Новое занятие: 10:30–12:00
        assertThatThrownBy(() -> lessonOverlapService.checkTeacherOverlap(
                TEACHER_ID,
                LESSON_DATE,
                LocalTime.of(10, 30),
                LocalTime.of(12, 0),
                null
        )).isInstanceOf(ConflictException.class)
                .hasMessageContaining("Преподаватель уже занят в это время");
    }

    private Lesson createLesson(Long id, LocalTime start, LocalTime end) {
        User teacher = new User();
        teacher.setId(TEACHER_ID);

        return Lesson.builder()
                .id(id)
                .teacher(teacher)
                .lessonDate(LESSON_DATE)
                .startTime(start)
                .endTime(end)
                .topic("Test Lesson")
                .description("Test description")
                .build();
    }
}