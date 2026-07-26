package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.lesson.LessonResponse;
import com.covenantcode.crm.entity.Lesson;
import com.covenantcode.crm.mapper.LessonMapper;
import com.covenantcode.crm.repository.LessonRepository;
import com.covenantcode.crm.utils.CurrentUserProvider;
import java.util.List;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonServiceImplTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonMapper lessonMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private LessonServiceImpl lessonService;

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
}
