package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.lesson.LessonResponse;
import com.covenantcode.crm.dto.lesson.LessonUpdateRequest;
import com.covenantcode.crm.entity.Lesson;
import com.covenantcode.crm.entity.StudyGroup;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.GroupStatus;
import com.covenantcode.crm.exception.BadRequestException;
import com.covenantcode.crm.exception.ConflictException;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.mapper.LessonMapper;
import com.covenantcode.crm.repository.LessonRepository;
import com.covenantcode.crm.repository.StudyGroupRepository;
import com.covenantcode.crm.repository.UserRepository;
import com.covenantcode.crm.service.LessonOverlapService;
import com.covenantcode.crm.utils.CurrentUserProvider;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonServiceImplTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonMapper lessonMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudyGroupRepository studyGroupRepository;

    @Mock
    private LessonOverlapService lessonOverlapService;

    @InjectMocks
    private LessonServiceImpl lessonService;

    // Вспомогательные переменные для тестов
    private final Long lessonId = 1L;
    private final Long teacherId = 10L;
    private final Long groupId = 100L;
    private final LocalDate lessonDate = LocalDate.of(2026, 9, 1);
    private final LocalTime startTime = LocalTime.of(9, 0);
    private final LocalTime endTime = LocalTime.of(11, 0);

    private LessonUpdateRequest createValidRequest() {
        LessonUpdateRequest request = new LessonUpdateRequest();
        request.setTeacherId(teacherId);
        request.setGroupId(groupId);
        request.setLessonDate(lessonDate);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setTopic("Тестовая тема занятия");
        return request;
    }

    private Lesson existingLesson;
    private StudyGroup activeGroup;
    private User teacher;

    @BeforeEach
    void setUp() {
        // Подготовка сущностей с использованием ваших констант
        activeGroup = new StudyGroup();
        activeGroup.setId(groupId);
        activeGroup.setStatus(GroupStatus.ACTIVE);

        teacher = new User();
        teacher.setId(teacherId);

        existingLesson = new Lesson();
        existingLesson.setId(lessonId);
        existingLesson.setStudyGroup(activeGroup);
        existingLesson.setTeacher(teacher);
    }

    @Test
    @DisplayName("Тест 1: без фильтров — возвращает все занятия постранично")
    void getAll_WithoutFilters_ReturnsAllLessons() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Lesson lesson1 = new Lesson();
        Lesson lesson2 = new Lesson();
        Page<Lesson> lessonPage = new PageImpl<>(List.of(lesson1, lesson2), pageable, 2);


        LessonResponse response1 = LessonResponse.builder().build();
        LessonResponse response2 = LessonResponse.builder().build();

        // Имитируем запрос от ADMIN / MANAGER, чтобы не срабатывала принудительная фильтрация преподавателя
        when(currentUserProvider.isTeacher()).thenReturn(false);

        // lessonRepository.findAll(any(Specification.class), any(Pageable.class)) → Page из двух занятий
        when(lessonRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(lessonPage);
        when(lessonMapper.toResponse(lesson1)).thenReturn(response1);
        when(lessonMapper.toResponse(lesson2)).thenReturn(response2);

        // When
        Page<LessonResponse> result = lessonService.getAll(null, null, null, null, pageable);

        // Then
        assertThat(result).isNotNull();
        // Проверить: метод вернул Page с двумя элементами
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).containsExactly(response1, response2);

        verify(lessonRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Тест 2: с фильтрами groupId и teacherId")
    void getAll_WithFilters_CallsRepositoryWithNonNullSpecification() {
        // Given
        Long groupId = 1L;
        Long teacherId = 2L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Lesson> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(currentUserProvider.isTeacher()).thenReturn(false);
        when(lessonRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        lessonService.getAll(groupId, teacherId, null, null, pageable);

        // Then
        // Убедиться, что findAll(spec, pageable) вызван один раз с ненулевой спецификацией
        verify(lessonRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Тест 1: успешное обновление занятия")
    void update_Success() {
        // Given
        LessonUpdateRequest request = createValidRequest();
        LessonResponse expectedResponse = LessonResponse.builder()
                .id(lessonId)
                .studyGroupId(groupId)
                .teacherId(teacherId)
                .build();

        // Подставляем значения из request, чтобы верификация совпала до миллисекунды
        LocalDate reqDate = request.getLessonDate();
        LocalTime reqStart = request.getStartTime();
        LocalTime reqEnd = request.getEndTime();

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(existingLesson));
        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(activeGroup));
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));

        when(lessonRepository.save(any(Lesson.class))).thenReturn(existingLesson);

        // ИСПРАВЛЕНО: используем any(Lesson.class), так как объект existingLesson мутирует внутри сервиса
        when(lessonMapper.toResponse(any(Lesson.class))).thenReturn(expectedResponse);

        // When
        LessonResponse actualResponse = lessonService.update(lessonId, request);

        // Then
        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);

        verify(lessonOverlapService).checkTeacherOverlap(teacherId, reqDate, reqStart, reqEnd, lessonId);
        verify(lessonRepository).save(any(Lesson.class));
    }

    @Test
    @DisplayName("Тест 2: группа занятия в статусе COMPLETED — BadRequestException")
    void update_ExistingGroupCompleted_ThrowsBadRequestException() {
        // Given
        LessonUpdateRequest request = createValidRequest();

        StudyGroup completedGroup = new StudyGroup();
        completedGroup.setId(groupId);
        completedGroup.setStatus(GroupStatus.COMPLETED);
        existingLesson.setStudyGroup(completedGroup);

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(existingLesson));

        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                lessonService.update(lessonId, request)
        );

        assertEquals("Нельзя редактировать занятия завершённой группы", exception.getMessage());

        verifyNoInteractions(studyGroupRepository, userRepository, lessonOverlapService);
        verify(lessonRepository, never()).save(any());
    }

    @Test
    @DisplayName("Тест 3: занятие не найдено — ResourceNotFoundException")
    void update_LessonNotFound_ThrowsResourceNotFoundException() {
        // Given
        LessonUpdateRequest request = createValidRequest();
        Long nonExistingId = 99L;

        when(lessonRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                lessonService.update(nonExistingId, request)
        );

        assertEquals("Lesson с id 99 не найдено", exception.getMessage());
        verifyNoInteractions(studyGroupRepository, userRepository, lessonOverlapService);
        verify(lessonRepository, never()).save(any());
    }

    @Test
    @DisplayName("Те bit 4: обновление без конфликта с самим собой")
    void update_NoConflictWithItself() {
        // Given
        LessonUpdateRequest request = createValidRequest();

        LocalDate reqDate = request.getLessonDate();
        LocalTime reqStart = request.getStartTime();
        LocalTime reqEnd = request.getEndTime();

        existingLesson.setLessonDate(reqDate);
        existingLesson.setStartTime(reqStart);
        existingLesson.setEndTime(reqEnd);

        LessonResponse mockResponse = mock(LessonResponse.class);

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(existingLesson));
        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(activeGroup));
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));

        when(lessonRepository.save(any(Lesson.class))).thenReturn(existingLesson);

        // ИСПРАВЛЕНО: any(Lesson.class) для стабильности при мутациях
        when(lessonMapper.toResponse(any(Lesson.class))).thenReturn(mockResponse);

        doNothing().when(lessonOverlapService).checkTeacherOverlap(teacherId, reqDate, reqStart, reqEnd, lessonId);

        // When & Then
        assertDoesNotThrow(() -> lessonService.update(lessonId, request));

        verify(lessonOverlapService).checkTeacherOverlap(teacherId, reqDate, reqStart, reqEnd, lessonId);
    }

    @Test
    @DisplayName("Тест 5: пересечение с другим занятием — ConflictException")
    void update_TeacherOverlap_ThrowsConflictException() {
        // Given
        LessonUpdateRequest request = createValidRequest();

        LocalDate reqDate = request.getLessonDate();
        LocalTime reqStart = request.getStartTime();
        LocalTime reqEnd = request.getEndTime();

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(existingLesson));
        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(activeGroup));
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));

        doThrow(new ConflictException("У преподавателя уже есть занятие в это время"))
                .when(lessonOverlapService).checkTeacherOverlap(teacherId, reqDate, reqStart, reqEnd, lessonId);

        // When & Then
        ConflictException exception = assertThrows(ConflictException.class, () ->
                lessonService.update(lessonId, request)
        );

        assertEquals("У преподавателя уже есть занятие в это время", exception.getMessage());
        verify(lessonRepository, never()).save(any());
    }
}
