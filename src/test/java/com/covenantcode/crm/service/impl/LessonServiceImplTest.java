package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.group.StudyGroupShortResponse;
import com.covenantcode.crm.dto.group.UserShortResponse;
import com.covenantcode.crm.dto.lesson.LessonCreateRequest;
import com.covenantcode.crm.dto.lesson.LessonResponse;
import com.covenantcode.crm.dto.lesson.LessonUpdateRequest;
import com.covenantcode.crm.entity.Lesson;
import com.covenantcode.crm.entity.Role;
import com.covenantcode.crm.entity.Student;
import com.covenantcode.crm.entity.StudyGroup;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.GroupStatus;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.exception.BadRequestException;
import com.covenantcode.crm.exception.ConflictException;
import com.covenantcode.crm.exception.ForbiddenException;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.mapper.LessonMapper;
import com.covenantcode.crm.repository.LessonRepository;
import com.covenantcode.crm.repository.StudentRepository;
import com.covenantcode.crm.repository.StudyGroupRepository;
import com.covenantcode.crm.repository.UserRepository;
import com.covenantcode.crm.service.LessonOverlapService;
import com.covenantcode.crm.utils.CurrentUserProvider;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private LessonServiceImpl lessonService;

    // Вспомогательные переменные для тестов
    private final Long lessonId = 1L;
    private final Long teacherId = 10L;
    private final Long groupId = 100L;
    private final LocalDate lessonDate = LocalDate.of(2026, 9, 1);
    private final LocalTime startTime = LocalTime.of(9, 0);
    private final LocalTime endTime = LocalTime.of(11, 0);

    private final Long GET_BY_ID_LESSON_ID = 1L;
    private final Long ADMIN_ID = 1L;
    private final Long TEACHER_ID = 3L;
    private final Long OTHER_TEACHER_ID = 5L;
    private final Long STUDENT_ID = 10L;
    private final Long GROUP_ID_FOR_GET = 100L;

    private Lesson lessonForGetById;
    private User adminUser;
    private User teacherForGetById;
    private User otherTeacherUser;
    private User studentUserEntity;
    private Student studentEntity;
    private StudyGroup studyGroupForGetById;
    private LessonResponse lessonResponseForGetById;
    private Role teacherRole;
    private Role adminRole;
    private Role studentRole;

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

        teacherRole = new Role();
        teacherRole.setId(1L);
        teacherRole.setName(RoleName.TEACHER);

        adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName(RoleName.ADMIN);

        studentRole = new Role();
        studentRole.setId(3L);
        studentRole.setName(RoleName.STUDENT);

        teacherForGetById = new User();
        teacherForGetById.setId(TEACHER_ID);
        teacherForGetById.setEmail("teacher@example.com");
        teacherForGetById.setRole(teacherRole);

        adminUser = new User();
        adminUser.setId(ADMIN_ID);
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(adminRole);

        otherTeacherUser = new User();
        otherTeacherUser.setId(OTHER_TEACHER_ID);
        otherTeacherUser.setEmail("other@example.com");
        otherTeacherUser.setRole(teacherRole);

        studentUserEntity = new User();
        studentUserEntity.setId(STUDENT_ID);
        studentUserEntity.setEmail("student@example.com");
        studentUserEntity.setRole(studentRole);

        studyGroupForGetById = new StudyGroup();
        studyGroupForGetById.setId(GROUP_ID_FOR_GET);
        studyGroupForGetById.setName("Test Group");
        studyGroupForGetById.setStudents(new HashSet<>());

        studentEntity = new Student();
        studentEntity.setId(1L);
        studentEntity.setUser(studentUserEntity);
        studentEntity.setStudyGroups(new HashSet<>());

        studyGroupForGetById.getStudents().add(studentEntity);
        studentEntity.getStudyGroups().add(studyGroupForGetById);

        lessonForGetById = new Lesson();
        lessonForGetById.setId(GET_BY_ID_LESSON_ID);
        lessonForGetById.setTeacher(teacherForGetById);
        lessonForGetById.setStudyGroup(studyGroupForGetById);
        lessonForGetById.setTopic("Test Lesson");
        lessonForGetById.setDescription("Test Description");
        lessonForGetById.setLessonDate(LocalDate.now());
        lessonForGetById.setStartTime(LocalTime.of(10, 0));
        lessonForGetById.setEndTime(LocalTime.of(11, 30));

        lessonResponseForGetById = LessonResponse.builder()
                .id(GET_BY_ID_LESSON_ID)
                .teacher(UserShortResponse.builder()
                        .id(TEACHER_ID)
                        .build())
                .studyGroup(StudyGroupShortResponse.builder()
                        .id(GROUP_ID_FOR_GET)
                        .build())
                .topic("Test Lesson")
                .description("Test Description")
                .lessonDate(LocalDate.now())
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 30))
                .build();
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
    @DisplayName("Тест 3: успешное обновление занятия")
    void update_Success() {
        // Given
        LessonUpdateRequest request = createValidRequest();

        LessonResponse expectedResponse = LessonResponse.builder()
                .id(lessonId)
                .studyGroup(StudyGroupShortResponse.builder()
                        .id(groupId)
                        .build())
                .teacher(UserShortResponse.builder()
                        .id(teacherId)
                        .build())
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
    @DisplayName("Тест 4: Группа не в статусе ACTIVE — BadRequestException")
    void update_ExistingGroupCompleted_ThrowsBadRequestException() {

        LessonUpdateRequest request = createValidRequest();

        StudyGroup completedGroup = new StudyGroup();
        completedGroup.setId(groupId);
        completedGroup.setStatus(GroupStatus.COMPLETED);

        existingLesson.setStudyGroup(completedGroup);

        when(lessonRepository.findById(lessonId))
                .thenReturn(Optional.of(existingLesson));

        when(studyGroupRepository.findById(groupId))
                .thenReturn(Optional.of(completedGroup));

        // When & Then
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> lessonService.update(lessonId, request)
        );

        assertEquals(
                "Группа студентов должна быть в статусе ACTIVE",
                exception.getMessage()
        );

        verify(studyGroupRepository).findById(groupId);
        verifyNoInteractions(userRepository, lessonOverlapService);
        verify(lessonRepository, never()).save(any());
    }

    @Test
    @DisplayName("Тест 5: занятие не найдено — ResourceNotFoundException")
    void update_LessonNotFound_ThrowsResourceNotFoundException() {

        LessonUpdateRequest request = createValidRequest();
        Long nonExistingId = 99L;

        when(lessonRepository.findById(nonExistingId))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> lessonService.update(nonExistingId, request)
        );

        assertEquals(
                "Lesson not found with id: 99",
                exception.getMessage()
        );

        verifyNoInteractions(
                studyGroupRepository,
                userRepository,
                lessonOverlapService
        );

        verify(lessonRepository, never()).save(any());
    }

    @Test
    @DisplayName("Тест 6: обновление без конфликта с самим собой")
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
    @DisplayName("Тест 7: пересечение с другим занятием — ConflictException")
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

    @Test
    @DisplayName("Тест 8: create — успешно создаёт занятие с группой и преподавателем")
    void create_ValidRequest_SavesLessonWithStudyGroupAndTeacher() {

        LessonCreateRequest request = createValidLessonCreateRequest();

        StudyGroup studyGroup = StudyGroup.builder()
                .id(request.getGroupId())
                .name("Java Group")
                .status(GroupStatus.ACTIVE)
                .build();

        User teacher = User.builder()
                .id(request.getTeacherId())
                .firstName("Ivan")
                .lastName("Ivanov")
                .email("teacher@test.com")
                .enabled(true)
                .build();

        Lesson mappedLesson = Lesson.builder()
                .topic(request.getTopic())
                .description(request.getDescription())
                .lessonDate(request.getLessonDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        Lesson savedLesson = Lesson.builder()
                .id(100L)
                .studyGroup(studyGroup)
                .teacher(teacher)
                .topic(request.getTopic())
                .description(request.getDescription())
                .lessonDate(request.getLessonDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        LessonResponse expectedResponse = LessonResponse.builder()
                .id(100L)
                .build();

        when(studyGroupRepository.findById(request.getGroupId()))
                .thenReturn(Optional.of(studyGroup));
        when(userRepository.findById(request.getTeacherId()))
                .thenReturn(Optional.of(teacher));
        when(lessonMapper.toEntity(request))
                .thenReturn(mappedLesson);
        when(lessonRepository.save(mappedLesson))
                .thenReturn(savedLesson);
        when(lessonMapper.toResponse(savedLesson))
                .thenReturn(expectedResponse);

        LessonResponse result = lessonService.create(request);

        assertThat(result).isEqualTo(expectedResponse);
        assertThat(mappedLesson.getStudyGroup()).isEqualTo(studyGroup);
        assertThat(mappedLesson.getTeacher()).isEqualTo(teacher);

        verify(studyGroupRepository, times(1)).findById(request.getGroupId());
        verify(userRepository, times(1)).findById(request.getTeacherId());

        verify(lessonOverlapService, times(1)).checkTeacherOverlap(
                eq(teacher.getId()),
                eq(request.getLessonDate()),
                eq(request.getStartTime()),
                eq(request.getEndTime()),
                isNull()
        );

        verify(lessonRepository, times(1)).save(mappedLesson);
        verify(lessonMapper, times(1)).toResponse(savedLesson);
    }

    @Test
    @DisplayName("Тест 9: create — если группа не найдена, выбрасывает ResourceNotFoundException")
    void create_GroupNotFound_ThrowsResourceNotFoundException() {

        LessonCreateRequest request = createValidLessonCreateRequest();

        when(studyGroupRepository.findById(request.getGroupId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(studyGroupRepository, times(1)).findById(request.getGroupId());

        verify(userRepository, never()).findById(any());
        verify(lessonOverlapService, never()).checkTeacherOverlap(any(), any(), any(), any(), any());
        verify(lessonMapper, never()).toEntity(any());
        verify(lessonRepository, never()).save(any());
    }

    @Test
    @DisplayName("Тест 10: create — если группа не ACTIVE, выбрасывает BadRequestException")
    void create_InactiveStudyGroup_ThrowsBadRequestException() {

        LessonCreateRequest request = createValidLessonCreateRequest();

        StudyGroup inactiveStudyGroup = StudyGroup.builder()
                .id(request.getGroupId())
                .name("Inactive Group")
                .status(GroupStatus.COMPLETED)
                .build();

        when(studyGroupRepository.findById(request.getGroupId()))
                .thenReturn(Optional.of(inactiveStudyGroup));

        assertThatThrownBy(() -> lessonService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Группа студентов должна быть в статусе ACTIVE");

        verify(studyGroupRepository, times(1)).findById(request.getGroupId());

        verify(userRepository, never()).findById(any());
        verify(lessonOverlapService, never()).checkTeacherOverlap(any(), any(), any(), any(), any());
        verify(lessonMapper, never()).toEntity(any());
        verify(lessonRepository, never()).save(any());
    }

    @Test
    @DisplayName("Тест 11: create — если время окончания раньше времени начала, выбрасывает BadRequestException")
    void create_EndTimeBeforeStartTime_ThrowsBadRequestException() {

        LessonCreateRequest request = createValidLessonCreateRequest();
        request.setStartTime(LocalTime.of(12, 0));
        request.setEndTime(LocalTime.of(10, 0));

        assertThatThrownBy(() -> lessonService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Время окончания урока должно быть позже времени начала");

        verify(studyGroupRepository, never()).findById(any());
        verify(userRepository, never()).findById(any());
        verify(lessonOverlapService, never()).checkTeacherOverlap(any(), any(), any(), any(), any());
        verify(lessonMapper, never()).toEntity(any());
        verify(lessonRepository, never()).save(any());
    }

    @Test
    @DisplayName("Тест 12: create — если время окончания равно времени начала, выбрасывает BadRequestException")
    void create_EndTimeEqualsStartTime_ThrowsBadRequestException() {

        LessonCreateRequest request = createValidLessonCreateRequest();
        request.setStartTime(LocalTime.of(10, 0));
        request.setEndTime(LocalTime.of(10, 0));

        assertThatThrownBy(() -> lessonService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Время окончания урока должно быть позже времени начала");

        verify(studyGroupRepository, never()).findById(any());
        verify(userRepository, never()).findById(any());
        verify(lessonOverlapService, never()).checkTeacherOverlap(any(), any(), any(), any(), any());
        verify(lessonMapper, never()).toEntity(any());
        verify(lessonRepository, never()).save(any());
    }

    @Test
    @DisplayName("Тест 13: create — если преподаватель не найден, выбрасывает ResourceNotFoundException")
    void create_TeacherNotFound_ThrowsResourceNotFoundException() {

        LessonCreateRequest request = createValidLessonCreateRequest();

        StudyGroup studyGroup = StudyGroup.builder()
                .id(request.getGroupId())
                .name("Java Group")
                .status(GroupStatus.ACTIVE)
                .build();

        when(studyGroupRepository.findById(request.getGroupId()))
                .thenReturn(Optional.of(studyGroup));
        when(userRepository.findById(request.getTeacherId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(studyGroupRepository, times(1)).findById(request.getGroupId());
        verify(userRepository, times(1)).findById(request.getTeacherId());

        verify(lessonOverlapService, never()).checkTeacherOverlap(any(), any(), any(), any(), any());
        verify(lessonMapper, never()).toEntity(any());
        verify(lessonRepository, never()).save(any());
    }

    @Test
    @DisplayName("Тест 14: create — если есть пересечение по преподавателю, исключение пробрасывается дальше")
    void create_TeacherOverlap_ThrowsException() {

        LessonCreateRequest request = createValidLessonCreateRequest();

        StudyGroup studyGroup = StudyGroup.builder()
                .id(request.getGroupId())
                .name("Java Group")
                .status(GroupStatus.ACTIVE)
                .build();

        User teacher = User.builder()
                .id(request.getTeacherId())
                .firstName("Ivan")
                .lastName("Ivanov")
                .email("teacher@test.com")
                .enabled(true)
                .build();

        when(studyGroupRepository.findById(request.getGroupId()))
                .thenReturn(Optional.of(studyGroup));
        when(userRepository.findById(request.getTeacherId()))
                .thenReturn(Optional.of(teacher));

        org.mockito.Mockito.doThrow(new BadRequestException("У преподавателя уже есть занятие в это время"))
                .when(lessonOverlapService)
                .checkTeacherOverlap(
                        eq(teacher.getId()),
                        eq(request.getLessonDate()),
                        eq(request.getStartTime()),
                        eq(request.getEndTime()),
                        isNull()
                );

        assertThatThrownBy(() -> lessonService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("У преподавателя уже есть занятие в это время");

        verify(studyGroupRepository, times(1)).findById(request.getGroupId());
        verify(userRepository, times(1)).findById(request.getTeacherId());

        verify(lessonOverlapService, times(1)).checkTeacherOverlap(
                eq(teacher.getId()),
                eq(request.getLessonDate()),
                eq(request.getStartTime()),
                eq(request.getEndTime()),
                isNull()
        );

        verify(lessonMapper, never()).toEntity(any());
        verify(lessonRepository, never()).save(any());
    }

    private LessonCreateRequest createValidLessonCreateRequest() {
        LessonCreateRequest request = new LessonCreateRequest();
        request.setGroupId(1L);
        request.setTeacherId(2L);
        request.setTopic("Java Collections");
        request.setDescription("Lesson about List, Set and Map");
        request.setLessonDate(LocalDate.of(2025, 1, 20));
        request.setStartTime(LocalTime.of(10, 0));
        request.setEndTime(LocalTime.of(11, 30));
        return request;
    }

    private void setupAuthenticationWithUser(User user) {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("Администратор получает занятие — успех")
    void getById_AdminUser_Success() {
        setupAuthenticationWithUser(adminUser);
        when(lessonRepository.findById(GET_BY_ID_LESSON_ID)).thenReturn(Optional.of(lessonForGetById));
        when(lessonMapper.toResponse(lessonForGetById)).thenReturn(lessonResponseForGetById);

        LessonResponse result = lessonService.getById(GET_BY_ID_LESSON_ID, authentication);

        assertNotNull(result);
        assertEquals(GET_BY_ID_LESSON_ID, result.getId());
        assertEquals(TEACHER_ID, result.getTeacher().getId());
        assertEquals("Test Lesson", result.getTopic());

        verify(lessonRepository).findById(GET_BY_ID_LESSON_ID);
        verify(lessonMapper).toResponse(lessonForGetById);
        verify(userRepository).findByEmail(adminUser.getEmail());
        verifyNoInteractions(studentRepository);
    }

    @Test
    @DisplayName("Преподаватель получает своё занятие — успех")
    void getById_TeacherOwnLesson_Success() {
        setupAuthenticationWithUser(teacherForGetById);
        when(lessonRepository.findById(GET_BY_ID_LESSON_ID)).thenReturn(Optional.of(lessonForGetById));
        when(lessonMapper.toResponse(lessonForGetById)).thenReturn(lessonResponseForGetById);

        LessonResponse result = lessonService.getById(GET_BY_ID_LESSON_ID, authentication);

        assertNotNull(result);
        assertEquals(GET_BY_ID_LESSON_ID, result.getId());
        assertEquals(TEACHER_ID, result.getTeacher().getId());

        verify(lessonRepository).findById(GET_BY_ID_LESSON_ID);
        verify(lessonMapper).toResponse(lessonForGetById);
        verify(userRepository).findByEmail(teacherForGetById.getEmail());
        verifyNoInteractions(studentRepository);
    }

    @Test
    @DisplayName("Преподаватель получает чужое занятие — ForbiddenException")
    void getById_TeacherOtherLesson_ThrowsForbiddenException() {
        setupAuthenticationWithUser(otherTeacherUser);
        when(lessonRepository.findById(GET_BY_ID_LESSON_ID)).thenReturn(Optional.of(lessonForGetById));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> lessonService.getById(GET_BY_ID_LESSON_ID, authentication)
        );
        assertEquals("У вас нет доступа к этому занятию", exception.getMessage());

        verify(lessonRepository).findById(GET_BY_ID_LESSON_ID);
        verify(userRepository).findByEmail(otherTeacherUser.getEmail());
        verifyNoInteractions(studentRepository);
        verify(lessonMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Студент получает занятие своей группы — успех")
    void getById_StudentOwnGroupLesson_Success() {
        setupAuthenticationWithUser(studentUserEntity);
        when(lessonRepository.findById(GET_BY_ID_LESSON_ID)).thenReturn(Optional.of(lessonForGetById));
        when(studentRepository.findByUser_Id(STUDENT_ID)).thenReturn(Optional.of(studentEntity));
        when(lessonMapper.toResponse(lessonForGetById)).thenReturn(lessonResponseForGetById);

        LessonResponse result = lessonService.getById(GET_BY_ID_LESSON_ID, authentication);

        assertNotNull(result);
        assertEquals(GET_BY_ID_LESSON_ID, result.getId());
        assertEquals(TEACHER_ID, result.getTeacher().getId());

        verify(lessonRepository).findById(GET_BY_ID_LESSON_ID);
        verify(userRepository).findByEmail(studentUserEntity.getEmail());
        verify(studentRepository).findByUser_Id(STUDENT_ID);
        verify(lessonMapper).toResponse(lessonForGetById);
    }

    @Test
    @DisplayName("Занятие не найдено — ResourceNotFoundException")
    void getById_LessonNotFound_ThrowsResourceNotFoundException() {
        Long nonExistentId = 99L;
        when(lessonRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> lessonService.getById(nonExistentId, authentication)
        );
        assertTrue(exception.getMessage().contains("Lesson"));
        assertTrue(exception.getMessage().contains(String.valueOf(nonExistentId)));

        verify(lessonRepository).findById(nonExistentId);
        verifyNoInteractions(userRepository);
        verifyNoInteractions(studentRepository);
        verify(lessonMapper, never()).toResponse(any());
    }
    @Test
    @DisplayName("Тест getLessonsByTeacher 1: ADMIN запрашивает расписание любого преподавателя — успех")
    void getLessonsByTeacher_WhenAdminRequests_ShouldReturnLessons() {
        // Given
        LocalDate dateFrom = lessonDate;
        LocalDate dateTo = lessonDate.plusDays(7);

        Lesson lesson2 = new Lesson();
        lesson2.setId(2L);
        List<Lesson> lessons = List.of(existingLesson, lesson2);

        LessonResponse response1 = LessonResponse.builder().id(lessonId).build();
        LessonResponse response2 = LessonResponse.builder().id(2L).build();

        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(lessonRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(lessons);
        when(lessonMapper.toResponse(existingLesson)).thenReturn(response1);
        when(lessonMapper.toResponse(lesson2)).thenReturn(response2);

        // Используем реальный безопасный токен для ADMIN (id = 999)
        User adminUser = new User();
        adminUser.setId(999L);
        Authentication authentication = new TestingAuthenticationToken(adminUser, null, "ROLE_ADMIN");

        // When
        List<LessonResponse> result = lessonService.getLessonsByTeacher(teacherId, dateFrom, dateTo, authentication);

        // Then
        assertThat(result).hasSize(2).containsExactly(response1, response2);
        verify(userRepository).findById(teacherId);
        verify(lessonRepository).findAll(any(Specification.class), any(Sort.class));
    }
    @Test
    @DisplayName("Тест 2: TEACHER запрашивает своё расписание — успех")
    void getLessonsByTeacher_WhenTeacherRequestsOwnSchedule_ShouldReturnLessons() {
        // Given
        LocalDate dateFrom = lessonDate;
        LocalDate dateTo = lessonDate.plusDays(7);

        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(lessonRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(Collections.emptyList());

        // Создаем токен для TEACHER (id = 10, совпадает с teacherId)
        Authentication authentication = new TestingAuthenticationToken(teacher, null, "ROLE_TEACHER");

        // When
        List<LessonResponse> result = lessonService.getLessonsByTeacher(teacherId, dateFrom, dateTo, authentication);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty()); // Корректный пустой список [] при отсутствии занятий согласно п. 4.3 ТЗ

        verify(lessonRepository).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    @DisplayName("Тест 3: TEACHER запрашивает расписание другого преподавателя — ForbiddenException")
    void getLessonsByTeacher_WhenTeacherRequestsSomeoneElsesSchedule_ShouldThrowForbiddenException() {
        // Given
        Long otherTeacherId = 555L;
        User otherTeacher = new User();
        otherTeacher.setId(otherTeacherId);

        // ИСПРАВЛЕНО: Задаем роль нашему преподавателю (teacher из setUp()),
        // чтобы currentUser.getRole().getName() не выбросил NullPointerException
        teacher.setRole(teacherRole);

        when(userRepository.findById(otherTeacherId)).thenReturn(Optional.of(otherTeacher));

        // Используем TestingAuthenticationToken или Mock, передавая обновленный объект teacher
        Authentication authentication = new TestingAuthenticationToken(teacher, null, "ROLE_TEACHER");

        // When & Then
        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> lessonService.getLessonsByTeacher(otherTeacherId, lessonDate, lessonDate, authentication)
        );

        assertEquals("Доступ к расписанию другого преподавателя запрещен", exception.getMessage());

        verify(lessonRepository, never()).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    @DisplayName("Тест 4: преподаватель не найден — ResourceNotFoundException")
    void getLessonsByTeacher_WhenTeacherNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        Long nonExistentTeacherId = 99L;
        LocalDate dateFrom = null;
        LocalDate dateTo = null;
        Authentication authentication = Mockito.mock(Authentication.class);

        // Мокаем репозиторий, чтобы он возвращал пустой Optional
        Mockito.when(userRepository.findById(nonExistentTeacherId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> lessonService.getLessonsByTeacher(nonExistentTeacherId, dateFrom, dateTo, authentication)
        );

        // Проверяем, что сообщение об ошибке строго совпадает с ТЗ
        Assertions.assertEquals("User с id 99 не найден", exception.getMessage());
    }

    @Test
    @DisplayName("Тест 1: успешное удаление занятия активной группы")
    void delete_Success_ActiveGroup() {
        // given
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(existingLesson));

        // when / then
        assertThatCode(() -> lessonService.delete(lessonId)).doesNotThrowAnyException();
        verify(lessonRepository, times(1)).delete(existingLesson);
    }

    @Test
    @DisplayName("Тест 2: группа в статусе COMPLETED — BadRequestException")
    void delete_GroupCompleted_ThrowsBadRequest() {
        StudyGroup completedGroup = new StudyGroup();
        completedGroup.setId(groupId);
        completedGroup.setStatus(GroupStatus.COMPLETED);

        Lesson lessonWithCompletedGroup = new Lesson();
        lessonWithCompletedGroup.setId(lessonId);
        lessonWithCompletedGroup.setStudyGroup(completedGroup);

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lessonWithCompletedGroup));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> lessonService.delete(lessonId)
        );
        assertEquals("Нельзя удалить занятие завершённой группы", exception.getMessage());

        verify(lessonRepository, never()).delete(any(Lesson.class));
    }

    @Test
    @DisplayName("Тест 3: занятие группы в статусе DRAFT — успешное удаление")
    void delete_Success_DraftGroup() {
        StudyGroup draftGroup = new StudyGroup();
        draftGroup.setId(groupId);
        draftGroup.setStatus(GroupStatus.DRAFT);

        Lesson lessonWithDraftGroup = new Lesson();
        lessonWithDraftGroup.setId(lessonId);
        lessonWithDraftGroup.setStudyGroup(draftGroup);

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lessonWithDraftGroup));

        assertThatCode(() -> lessonService.delete(lessonId)).doesNotThrowAnyException();
        verify(lessonRepository, times(1)).delete(lessonWithDraftGroup);
    }

    @Test
    @DisplayName("Тест 4: занятие не найдено — ResourceNotFoundException")
    void delete_LessonNotFound_ThrowsResourceNotFound() {
        Long nonExistentId = 99L;
        when(lessonRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> lessonService.delete(nonExistentId)
        );
        assertTrue(exception.getMessage().contains("Lesson"));
        assertTrue(exception.getMessage().contains(String.valueOf(nonExistentId)));

        verify(lessonRepository, never()).delete(any(Lesson.class));
    }
}
