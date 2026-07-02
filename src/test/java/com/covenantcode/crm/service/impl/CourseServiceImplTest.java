package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.course.CourseCreateRequest;
import com.covenantcode.crm.dto.course.CourseResponse;
import com.covenantcode.crm.dto.course.CourseUpdateRequest;
import com.covenantcode.crm.entity.Course;
import com.covenantcode.crm.entity.enums.CourseStatus;
import com.covenantcode.crm.entity.enums.GroupStatus;
import com.covenantcode.crm.exception.ConflictException;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.mapper.CourseMapper;
import com.covenantcode.crm.repository.CourseRepository;
import com.covenantcode.crm.repository.StudyGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;


@ExtendWith(MockitoExtension.class)
public class CourseServiceImplTest {

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private StudyGroupRepository studyGroupRepository;

    @InjectMocks
    private CourseServiceImpl courseService;

    private CourseCreateRequest request;
    private Course course;
    private Course savedCourse;
    private CourseResponse response;
    private CourseResponse courseResponse;
    private Course existingCourse;

    @BeforeEach
    void setUp() {
        request = new CourseCreateRequest();
        request.setTitle("Java для начинающих");
        request.setDescription("Полный курс по Java");
        request.setDurationInWeeks(16);
        request.setPrice(new BigDecimal("45000.00"));
        request.setStatus(CourseStatus.ACTIVE);

        course = new Course();
        course.setId(1L);
        course.setTitle("Java Developer");
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setDurationInWeeks(request.getDurationInWeeks());
        course.setPrice(request.getPrice());
        course.setStatus(CourseStatus.ACTIVE);
        savedCourse = new Course();
        savedCourse.setId(1L);
        savedCourse.setTitle(request.getTitle());
        savedCourse.setDescription(request.getDescription());
        savedCourse.setDurationInWeeks(request.getDurationInWeeks());
        savedCourse.setPrice(request.getPrice());
        savedCourse.setStatus(CourseStatus.ACTIVE);
        savedCourse.setCreatedAt(OffsetDateTime.now());
        savedCourse.setUpdatedAt(OffsetDateTime.now());

        response = CourseResponse.builder()
                .id(1L)
                .title("Java Developer")
                .title(request.getTitle())
                .description(request.getDescription())
                .durationInWeeks(request.getDurationInWeeks())
                .price(request.getPrice())
                .status("ACTIVE")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        existingCourse = new Course();
        existingCourse.setId(1L);
        existingCourse.setTitle("Существующий курс");
        existingCourse.setDescription("Описание существующего курса");
        existingCourse.setDurationInWeeks(12);
        existingCourse.setPrice(new BigDecimal("35000.00"));
        existingCourse.setStatus(CourseStatus.ACTIVE);
        existingCourse.setCreatedAt(OffsetDateTime.now());
        existingCourse.setUpdatedAt(OffsetDateTime.now());
    }

    @Test
    public void create_success_withAllFields() {
        when(courseMapper.toEntity(request)).thenReturn(course);
        when(courseRepository.save(course)).thenReturn(savedCourse);
        when(courseMapper.toResponse(savedCourse)).thenReturn(response);
        CourseResponse result = courseService.create(request);
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("ACTIVE", result.getStatus());

        verify(courseRepository).save(course);
    }

    @Test
    public void create_statusNotProvided_setsActive() {
        request.setStatus(null);
        Course courseWithoutStatus = new Course();
        courseWithoutStatus.setTitle(request.getTitle());
        courseWithoutStatus.setDescription(request.getDescription());
        courseWithoutStatus.setDurationInWeeks(request.getDurationInWeeks());
        courseWithoutStatus.setPrice(request.getPrice());
        courseWithoutStatus.setStatus(null);

        when(courseMapper.toEntity(any(CourseCreateRequest.class))).thenReturn(courseWithoutStatus);
        when(courseRepository.save(any(Course.class))).thenReturn(savedCourse);
        when(courseMapper.toResponse(savedCourse)).thenReturn(response);

        CourseResponse result = courseService.create(request);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        assertEquals(CourseStatus.ACTIVE, captor.getValue().getStatus());

        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());
    }

    @Test
    void getById_WhenCourseExists_ShouldReturnResponse() {

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(courseMapper.toResponse(course)).thenReturn(response);

        CourseResponse result = courseService.getById(1L);
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Java для начинающих", result.getTitle());
        verify(courseRepository, times(1)).findById(1L);
        verify(courseMapper, times(1)).toResponse(course);
    }

    @Test
    void getById_WhenCourseDoesNotExist_ShouldThrowException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> courseService.getById(999L));
        verify(courseRepository, times(1)).findById(999L);
        verify(courseMapper, never()).toResponse(any());
    }

    @Test
    public void delete_success_whenCourseExistsAndNoActiveGroups() {
        Long courseId = 1L;
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(studyGroupRepository.existsByCourseIdAndStatus(courseId, GroupStatus.ACTIVE)).thenReturn(false);

        assertDoesNotThrow(() -> courseService.delete(courseId));

        verify(courseRepository).findById(courseId);
        verify(studyGroupRepository).existsByCourseIdAndStatus(courseId, GroupStatus.ACTIVE);
        verify(courseRepository).deleteById(courseId);
    }

    @Test
    public void delete_throwsResourceNotFoundException_whenCourseNotFound() {
        Long nonExistentCourseId = 999L;
        when(courseRepository.findById(nonExistentCourseId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> courseService.delete(nonExistentCourseId));

        verify(studyGroupRepository, never()).existsByCourseIdAndStatus(any(Long.class), any(GroupStatus.class));

        verify(courseRepository, never()).deleteById(any(Long.class));
    }

    @Test
    public void delete_throwsConflictException_whenCourseHasActiveGroups() {
        Long courseId = 1L;
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(existingCourse));
        when(studyGroupRepository.existsByCourseIdAndStatus(courseId, GroupStatus.ACTIVE)).thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> courseService.delete(courseId)
        );

        assertEquals("Невозможно удалить курс: существуют активные учебные группы", exception.getMessage());

        verify(courseRepository, never()).deleteById(any(Long.class));

        verify(courseRepository).findById(courseId);

        verify(studyGroupRepository).existsByCourseIdAndStatus(courseId, GroupStatus.ACTIVE);
    }

    @Test
    void update_shouldReturnUpdatedCourse_whenCourseExists() {
        Long id = 1L;

        CourseUpdateRequest request = new CourseUpdateRequest();
        request.setTitle("New title");
        request.setPrice(new BigDecimal("199.99"));
        request.setDurationInWeeks(12);

        Course course = new Course();
        course.setId(id);
        course.setTitle("Old title");
        course.setPrice(new BigDecimal("99.99"));
        course.setDurationInWeeks(8);

        Course updatedCourse = new Course();
        updatedCourse.setId(id);
        updatedCourse.setTitle("New title");
        updatedCourse.setPrice(new BigDecimal("199.99"));
        updatedCourse.setDurationInWeeks(12);

        CourseResponse response = new CourseResponse();
        response.setId(id);
        response.setTitle("New title");
        response.setPrice(new BigDecimal("199.99"));
        response.setDurationInWeeks(12);

        when(courseRepository.findById(id)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenReturn(updatedCourse);
        when(courseMapper.toResponse(updatedCourse)).thenReturn(response);

        CourseResponse result = courseService.update(id, request);

        assertNotNull(result);
        assertEquals("New title", result.getTitle());
        assertEquals(new BigDecimal("199.99"), result.getPrice());

        verify(courseRepository, times(1)).save(any(Course.class));
    }

    @Test
    void update_shouldThrowResourceNotFoundException_whenCourseNotFound() {
        Long id = 999L;

        CourseUpdateRequest request = new CourseUpdateRequest();
        request.setTitle("New title");
        request.setPrice(new BigDecimal("199.99"));
        request.setDurationInWeeks(12);

        when(courseRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> courseService.update(id, request));

        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Получение списка курсов — без поиска и статуса возвращаются все курсы")
    void findAll_whenSearchNullAndStatusNull_returnsAllCoursesWithoutFiltering() {
        Pageable pageable = PageRequest.of(0, 10);

        Course course1 = course(1L, "Java Basics", CourseStatus.ACTIVE);
        Course course2 = course(2L, "Spring Boot", CourseStatus.ARCHIVED);

        Page<Course> expectedPage = new PageImpl<>(List.of(course1, course2), pageable, 2);

        when(courseRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(expectedPage);

        Page<CourseResponse> result = courseService.getAll(null, null, pageable);

        assertThat(result.getContent()).hasSize(2);
        verify(courseRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Получение списка курсов — поиск только по названию, если статус не указан")
    void findAll_whenSearchJavaAndStatusNull_filtersOnlyByText() {
        Pageable pageable = PageRequest.of(0, 10);

        Course course = course(1L, "Java Basics", CourseStatus.ACTIVE);
        Page<Course> expectedPage = new PageImpl<>(List.of(course), pageable, 1);

        when(courseRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(expectedPage);
        when(courseMapper.toResponse(any(Course.class))).thenReturn(
                CourseResponse.builder()
                        .id(1L)
                        .title("Java Basics")
                        .status(CourseStatus.ACTIVE.name())
                        .build()
        );

        Page<CourseResponse> result = courseService.getAll("java", null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).containsIgnoringCase("java");
        verify(courseRepository).findAll(any(Specification.class), eq(pageable));
        verify(courseMapper).toResponse(any(Course.class));
    }

    @Test
    @DisplayName("Получение списка курсов — фильтрация только по статусу, если поиск не задан")
    void findAll_whenSearchNullAndStatusActive_filtersOnlyByStatus() {
        Pageable pageable = PageRequest.of(0, 10);

        Course course = course(1L, "Java Basics", CourseStatus.ACTIVE);
        Page<Course> expectedPage = new PageImpl<>(List.of(course), pageable, 1);

        when(courseRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(expectedPage);
        when(courseMapper.toResponse(any(Course.class))).thenReturn(
                CourseResponse.builder()
                        .id(1L)
                        .title("Java Basics")
                        .status(CourseStatus.ACTIVE.name())
                        .build()
        );

        Page<CourseResponse> result = courseService.getAll(null, CourseStatus.ACTIVE, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent()).allMatch(c -> c.getStatus().equals(CourseStatus.ACTIVE.name()));
        verify(courseRepository).findAll(any(Specification.class), eq(pageable));
        verify(courseMapper).toResponse(any(Course.class));
    }

    @Test
    @DisplayName("Получение списка курсов — поиск и статус применяются одновременно")
    void findAll_whenSearchJavaAndStatusActive_appliesBothFilters() {
        Pageable pageable = PageRequest.of(0, 10);

        Course course = course(1L, "Java Basics", CourseStatus.ACTIVE);
        Page<Course> expectedPage = new PageImpl<>(List.of(course), pageable, 1);

        when(courseRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(expectedPage);
        when(courseMapper.toResponse(any(Course.class))).thenReturn(
                CourseResponse.builder()
                        .id(1L)
                        .title("Java Basics")
                        .status(CourseStatus.ACTIVE.name())
                        .build()
        );

        Page<CourseResponse> result = courseService.getAll("java", CourseStatus.ACTIVE, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).containsIgnoringCase("java");
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(CourseStatus.ACTIVE.name());
        verify(courseRepository).findAll(any(Specification.class), eq(pageable));
        verify(courseMapper).toResponse(any(Course.class));
    }

    private Course course(Long id, String name, CourseStatus status) {
        Course course = new Course();
        course.setId(id);
        course.setTitle(name);
        course.setStatus(status);
        return course;
    }
}
