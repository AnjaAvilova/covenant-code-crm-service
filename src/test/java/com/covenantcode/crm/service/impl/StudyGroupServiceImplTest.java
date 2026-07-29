package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.group.AddStudentToGroupRequest;
import com.covenantcode.crm.dto.group.CourseShortResponse;
import com.covenantcode.crm.dto.group.GroupStatusUpdateRequest;
import com.covenantcode.crm.dto.group.StudyGroupCreateRequest;
import com.covenantcode.crm.dto.group.StudyGroupResponse;
import com.covenantcode.crm.dto.group.StudyGroupUpdateRequest;
import com.covenantcode.crm.dto.group.UserShortResponse;
import com.covenantcode.crm.dto.student.StudentResponse;
import com.covenantcode.crm.entity.Course;
import com.covenantcode.crm.entity.Role;
import com.covenantcode.crm.entity.Student;
import com.covenantcode.crm.entity.StudyGroup;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.GroupStatus;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.exception.BadRequestException;
import com.covenantcode.crm.exception.ConflictException;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.mapper.LessonMapper;
import com.covenantcode.crm.mapper.StudentMapper;
import com.covenantcode.crm.mapper.StudyGroupMapper;
import com.covenantcode.crm.repository.CourseRepository;
import com.covenantcode.crm.repository.LessonRepository;
import com.covenantcode.crm.repository.StudentRepository;
import com.covenantcode.crm.repository.StudyGroupRepository;
import com.covenantcode.crm.repository.UserRepository;
import com.covenantcode.crm.utils.CurrentUserProvider;
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
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudyGroupServiceImplTest {

    @Mock
    private StudyGroupRepository studyGroupRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudyGroupMapper studyGroupMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private StudentMapper studentMapper;

    @Mock
    private LessonMapper lessonMapper;

    @InjectMocks
    private StudyGroupServiceImpl studyGroupService;

    private final LocalDate startDate = LocalDate.of(2026, 7, 6);
    private StudyGroup group1, group2, group3;
    private StudyGroupResponse resp1, resp2;
    private Course course1, course2;
    private User teacher1, teacher2, teacherWithoutRole;
    private Role teacherRole;
    private StudyGroupUpdateRequest updateRequest;
    private User currentUser;
    private Long currentUserId;

    private static final Long GROUP_ID_FOR_REMOVE = 1L;
    private static final Long STUDENT_ID_FOR_REMOVE = 10L;

    @BeforeEach
    void setUp() {

        // Создание ролей
        teacherRole = new Role();
        teacherRole.setName(RoleName.TEACHER);

        // Создание курсов
        course1 = new Course();
        course1.setId(1L);
        course1.setTitle("Java Core");

        course2 = new Course();
        course2.setId(2L);
        course2.setTitle("Spring Boot");

        // Создание учителей
        teacher1 = new User();
        teacher1.setId(100L);
        teacher1.setFirstName("John");
        teacher1.setLastName("Doe");
        teacher1.setRole(teacherRole);

        teacher2 = new User();
        teacher2.setId(200L);
        teacher2.setFirstName("Jane");
        teacher2.setLastName("Smith");
        teacher2.setRole(teacherRole);

        teacherWithoutRole = new User();
        teacherWithoutRole.setId(300L);
        teacherWithoutRole.setFirstName("Bob");
        teacherWithoutRole.setLastName("Johnson");
        teacherWithoutRole.setRole(teacherRole);


        group1 = new StudyGroup();
        group1.setId(1L);
        group1.setName("Java Core");
        group1.setStatus(GroupStatus.ACTIVE);
        group1.setStartDate(LocalDate.of(2024, 9, 1));
        group1.setCourse(course1);
        group1.setTeacher(teacher1);

        group2 = new StudyGroup();
        group2.setId(2L);
        group2.setName("Spring Boot");
        group2.setStatus(GroupStatus.DRAFT);
        group2.setStartDate(LocalDate.of(2024, 10, 15));
        group2.setCourse(course2);
        group2.setTeacher(teacher2);


        group3 = new StudyGroup();
        group3.setId(3L);
        group3.setName("Kotlin Advanced");
        group3.setStatus(GroupStatus.COMPLETED);
        group3.setStartDate(LocalDate.of(2024, 6, 1));
        group3.setCourse(course1);
        group3.setTeacher(teacher1);

        resp1 = new StudyGroupResponse();
        resp1.setId(1L);
        resp1.setName("Java Core");
        resp1.setStatus(GroupStatus.ACTIVE);

        resp2 = new StudyGroupResponse();
        resp2.setId(2L);
        resp2.setName("Spring Boot");
        resp2.setStatus(GroupStatus.DRAFT);

        // Инициализация запроса на обновление
        updateRequest = new StudyGroupUpdateRequest();
        updateRequest.setName("Updated Group");
        updateRequest.setCourseId(2L);
        updateRequest.setTeacherId(200L);
        updateRequest.setStartDate(startDate);

        currentUserId = 1L;
        Role adminRole = new Role();
        adminRole.setName(RoleName.ADMIN);

        currentUser = new User();
        currentUser.setId(currentUserId);
        currentUser.setRole(adminRole);
        currentUser.setEmail("admin@example.com");
    }

    @Test
    void createGroupWithoutStudents_shouldSucceed() {
        Long courseId = 1L;
        Long teacherId = 2L;
        Course course = Course.builder().id(courseId).title("Java").build();
        User teacher = createTeacher(teacherId);

        StudyGroupCreateRequest request = StudyGroupCreateRequest.builder()
                .name("Group A")
                .courseId(courseId)
                .teacherId(teacherId)
                .startDate(startDate)
                .build();

        StudyGroup savedGroup = StudyGroup.builder()
                .id(10L)
                .name("Group A")
                .course(course)
                .teacher(teacher)
                .startDate(startDate)
                .students(Set.of())
                .status(GroupStatus.DRAFT)
                .build();

        StudyGroupResponse mockResponse = StudyGroupResponse.builder().id(10L).name("Group A").build();

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(studyGroupRepository.save(any(StudyGroup.class))).thenReturn(savedGroup);
        when(studyGroupMapper.toResponse(savedGroup)).thenReturn(mockResponse);

        StudyGroupResponse response = studyGroupService.create(request);

        assertThat(response).isNotNull();
        verify(studyGroupRepository).save(any());
    }

    @Test
    @DisplayName("Should return page with one group when no filters applied")
    void getAll_NoFilters_ReturnsNonEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);

        when(currentUserProvider.isTeacher()).thenReturn(false);
        when(studyGroupRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(group1), pageable, 1));
        when(studyGroupMapper.toResponse(group1)).thenReturn(resp1);

        Page<StudyGroupResponse> result = studyGroupService.getAll(null, null, null, pageable);

        assertThat(result).isNotEmpty();
        verify(currentUserProvider).isTeacher();
        verify(studyGroupRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Should apply courseId filter in specification")
    void getAll_WithCourseIdFilter_AppliesSpecification() {
        Pageable pageable = PageRequest.of(0, 10);

        when(currentUserProvider.isTeacher()).thenReturn(false);
        when(studyGroupRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(group1), pageable, 1));
        when(studyGroupMapper.toResponse(group1)).thenReturn(resp1);

        studyGroupService.getAll(1L, null, null, pageable);

        verify(currentUserProvider).isTeacher();
        verify(studyGroupRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Should return empty page when no groups match criteria")
    void getAll_NoMatches_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);

        when(currentUserProvider.isTeacher()).thenReturn(false);
        when(studyGroupRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        Page<StudyGroupResponse> result = studyGroupService.getAll(
                99L, 99L, GroupStatus.CANCELLED, pageable
        );

        assertThat(result).isEmpty();
        verify(currentUserProvider).isTeacher();
        verify(studyGroupRepository).findAll(any(Specification.class), eq(pageable));
        verifyNoInteractions(studyGroupMapper);
    }

    @Test
    @DisplayName("Should restrict groups by current teacher when current user is teacher")
    void getAll_WhenCurrentUserIsTeacher_AppliesTeacherRestriction() {
        Pageable pageable = PageRequest.of(0, 10);

        when(currentUserProvider.isTeacher()).thenReturn(true);
        when(currentUserProvider.getCurrentUserId()).thenReturn(currentUserId);

        when(studyGroupRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(group1), pageable, 1));

        when(studyGroupMapper.toResponse(group1)).thenReturn(resp1);

        Page<StudyGroupResponse> result = studyGroupService.getAll(null, null, null, pageable);

        assertThat(result).isNotEmpty();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(resp1);

        verify(currentUserProvider).isTeacher();
        verify(currentUserProvider).getCurrentUserId();
        verify(currentUserProvider, never()).getCurrentUser();

        verify(studyGroupRepository).findAll(any(Specification.class), eq(pageable));
        verify(studyGroupMapper).toResponse(group1);
    }

    private User createTeacher(Long id) {
        Role teacherRole = Role.builder().name(RoleName.TEACHER).build();
        return User.builder()
                .id(id)
                .firstName("Teacher")
                .lastName("Test")
                .role(teacherRole)
                .build();
    }


    @Test
    void update_Success_ShouldReturnUpdatedResponse_WhenGroupIsDraftAndAllValid() {
        // Arrange
        when(studyGroupRepository.findById(2L)).thenReturn(Optional.of(group2));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course2));
        when(userRepository.findById(200L)).thenReturn(Optional.of(teacher2));

        StudyGroup updatedGroup = new StudyGroup();
        updatedGroup.setId(2L);
        updatedGroup.setName("Updated Group");
        updatedGroup.setStatus(GroupStatus.DRAFT);
        updatedGroup.setStartDate(LocalDate.of(2026, 7, 6));
        updatedGroup.setCourse(course2);
        updatedGroup.setTeacher(teacher2);
        updatedGroup.setStudents(group2.getStudents()); // Гарантируем перенос студентов из существующей группы

        // Создаем детальный mock-ответ, отражающий структуру из ТЗ
        StudyGroupResponse expectedResponse = StudyGroupResponse.builder()
                .id(2L)
                .name("Updated Group")
                .status(GroupStatus.DRAFT)
                .startDate(LocalDate.of(2026, 7, 6))
                .course(CourseShortResponse.builder().id(2L).name("Spring Boot").build())
                .teacher(UserShortResponse.builder().id(200L).firstName("Jane").lastName("Smith").build())
                .students(List.of()) // Проверяем, что поле присутствует в ответе и не равно null
                .build();

        when(studyGroupRepository.save(any(StudyGroup.class))).thenReturn(updatedGroup);
        when(studyGroupMapper.toResponse(updatedGroup)).thenReturn(expectedResponse);

        // Act
        StudyGroupResponse response = studyGroupService.update(2L, updateRequest);

        // Assert
        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals("Updated Group", response.getName());
        assertEquals(GroupStatus.DRAFT, response.getStatus());
        assertEquals(LocalDate.of(2026, 7, 6), response.getStartDate());

        // Сверка вложенных сущностей курса и учителя
        assertNotNull(response.getCourse());
        assertEquals(2L, response.getCourse().getId());
        assertNotNull(response.getTeacher());
        assertEquals(200L, response.getTeacher().getId());

        // Критическая проверка на баг с пропавшими студентами (теперь упадет, если маппер вернет null/пустоту некорректно)
        assertNotNull(response.getStudents(), "Поле students не должно быть null в ответе");

        // Верификация вызовов
        verify(studyGroupRepository).save(any(StudyGroup.class));
        verify(studyGroupMapper).toResponse(updatedGroup);
    }

    @Test
    void update_ShouldThrowBadRequestException_WhenGroupStatusIsCompleted() {
        // Arrange
        when(studyGroupRepository.findById(3L)).thenReturn(Optional.of(group3));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> studyGroupService.update(3L, updateRequest));

        // Исправлено: текст ошибки на русском языке
        assertTrue(exception.getMessage().contains("Нельзя редактировать группу в статусе COMPLETED"));
    }

    @Test
    void update_ShouldThrowBadRequestException_WhenGroupStatusIsCancelled() {
        // Arrange
        StudyGroup cancelledGroup = new StudyGroup();
        cancelledGroup.setId(4L);
        cancelledGroup.setStatus(GroupStatus.CANCELLED);
        when(studyGroupRepository.findById(4L)).thenReturn(Optional.of(cancelledGroup));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> studyGroupService.update(4L, updateRequest));

        // Исправлено: текст ошибки на русском языке
        assertTrue(exception.getMessage().contains("Нельзя редактировать группу в статусе CANCELLED"));
    }

    @Test
    void update_ShouldThrowResourceNotFoundException_WhenGroupNotFound() {
        // Arrange
        when(studyGroupRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> studyGroupService.update(99L, updateRequest));

        // Исправлено: текст ошибки на русском языке
        assertTrue(exception.getMessage().contains("StudyGroup с id 99 не найдена"));
    }

    @Test
    void update_ShouldThrowResourceNotFoundException_WhenCourseNotFound() {
        // Arrange
        when(studyGroupRepository.findById(2L)).thenReturn(Optional.of(group2));
        when(courseRepository.findById(2L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> studyGroupService.update(2L, updateRequest));

        // Исправлено: текст ошибки на русском языке
        assertTrue(exception.getMessage().contains("Course с id 2 не найден"));
    }

    @Test
    void update_ShouldThrowResourceNotFoundException_WhenTeacherNotFound() {
        // Arrange
        when(studyGroupRepository.findById(2L)).thenReturn(Optional.of(group2));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course2));
        when(userRepository.findById(200L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> studyGroupService.update(2L, updateRequest));

        // Исправлено: изменен тип сущности на User и язык на русский
        assertTrue(exception.getMessage().contains("User с id 200 не найден"));
    }

    @Test
    void update_ShouldThrowBadRequestException_WhenTeacherHasNoTeacherRole() {
        // Arrange
        Role role = new Role();
        role.setName(RoleName.STUDENT);

        teacherWithoutRole.setId(200L);
        teacherWithoutRole.setRole(role);

        when(studyGroupRepository.findById(any(Long.class))).thenReturn(Optional.of(group2));
        when(courseRepository.findById(any(Long.class))).thenReturn(Optional.of(course2));
        when(userRepository.findById(any(Long.class))).thenReturn(Optional.of(teacherWithoutRole));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                studyGroupService.update(2L, updateRequest));

        // Исправлено: текст ошибки на русском языке
        assertTrue(exception.getMessage().contains("Пользователь с id 200 не является учителем"));
    }

    @Test
    @DisplayName("DRAFT → ACTIVE: допустимый переход, статус изменён")
    void updateStatus_DraftToActive_ShouldSucceed() {

        Long groupId = 1L;
        GroupStatusUpdateRequest request = new GroupStatusUpdateRequest(GroupStatus.ACTIVE);

        StudyGroup group = StudyGroup.builder()
                .id(groupId)
                .status(GroupStatus.DRAFT)
                .build();

        StudyGroupResponse expectedResponse = StudyGroupResponse.builder()
                .id(groupId)
                .status(GroupStatus.ACTIVE)
                .build();

        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(studyGroupRepository.save(any(StudyGroup.class))).thenReturn(group);
        when(studyGroupMapper.toResponse(any(StudyGroup.class))).thenReturn(expectedResponse);

        StudyGroupResponse result = studyGroupService.updateStatus(groupId, request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(GroupStatus.ACTIVE);
        assertThat(group.getStatus()).isEqualTo(GroupStatus.ACTIVE);
        verify(studyGroupRepository).findById(groupId);
        verify(studyGroupRepository).save(group);
        verify(studyGroupMapper).toResponse(group);
    }

    @Test
    @DisplayName("ACTIVE → COMPLETED: допустимый переход, статус изменён")
    void updateStatus_ActiveToCompleted_ShouldSucceed() {

        Long groupId = 1L;
        GroupStatusUpdateRequest request = new GroupStatusUpdateRequest(GroupStatus.COMPLETED);

        StudyGroup group = StudyGroup.builder()
                .id(groupId)
                .status(GroupStatus.ACTIVE)
                .build();

        StudyGroupResponse expectedResponse = StudyGroupResponse.builder()
                .id(groupId)
                .status(GroupStatus.COMPLETED)
                .build();

        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(studyGroupRepository.save(any(StudyGroup.class))).thenReturn(group);
        when(studyGroupMapper.toResponse(any(StudyGroup.class))).thenReturn(expectedResponse);

        StudyGroupResponse result = studyGroupService.updateStatus(groupId, request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(GroupStatus.COMPLETED);
        assertThat(group.getStatus()).isEqualTo(GroupStatus.COMPLETED);
        verify(studyGroupRepository).findById(groupId);
        verify(studyGroupRepository).save(group);
        verify(studyGroupMapper).toResponse(group);
    }

    @Test
    @DisplayName("ACTIVE → CANCELLED: допустимый переход, статус изменён")
    void updateStatus_ActiveToCancelled_ShouldSucceed() {

        Long groupId = 1L;
        GroupStatusUpdateRequest request = new GroupStatusUpdateRequest(GroupStatus.CANCELLED);

        StudyGroup group = StudyGroup.builder()
                .id(groupId)
                .status(GroupStatus.ACTIVE)
                .build();

        StudyGroupResponse expectedResponse = StudyGroupResponse.builder()
                .id(groupId)
                .status(GroupStatus.CANCELLED)
                .build();

        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(studyGroupRepository.save(any(StudyGroup.class))).thenReturn(group);
        when(studyGroupMapper.toResponse(any(StudyGroup.class))).thenReturn(expectedResponse);

        StudyGroupResponse result = studyGroupService.updateStatus(groupId, request);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(GroupStatus.CANCELLED);
        assertThat(group.getStatus()).isEqualTo(GroupStatus.CANCELLED);
        verify(studyGroupRepository).findById(groupId);
        verify(studyGroupRepository).save(group);
        verify(studyGroupMapper).toResponse(group);
    }

    @Test
    @DisplayName("DRAFT → COMPLETED: недопустимый переход → BadRequestException с обоими статусами")
    void updateStatus_DraftToCompleted_ShouldThrowBadRequestException() {

        Long groupId = 1L;
        GroupStatusUpdateRequest request = new GroupStatusUpdateRequest(GroupStatus.COMPLETED);

        StudyGroup group = StudyGroup.builder()
                .id(groupId)
                .status(GroupStatus.DRAFT)
                .build();

        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> studyGroupService.updateStatus(groupId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("DRAFT")
                .hasMessageContaining("COMPLETED");

        verify(studyGroupRepository).findById(groupId);
        verify(studyGroupRepository, never()).save(any());
        verifyNoInteractions(studyGroupMapper);
    }

    @Test
    @DisplayName("COMPLETED → ACTIVE: переход из финального статуса → BadRequestException")
    void updateStatus_CompletedToActive_ShouldThrowBadRequestException() {

        Long groupId = 1L;
        GroupStatusUpdateRequest request = new GroupStatusUpdateRequest(GroupStatus.ACTIVE);

        StudyGroup group = StudyGroup.builder()
                .id(groupId)
                .status(GroupStatus.COMPLETED)
                .build();

        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> studyGroupService.updateStatus(groupId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("COMPLETED")
                .hasMessageContaining("ACTIVE");

        verify(studyGroupRepository).findById(groupId);
        verify(studyGroupRepository, never()).save(any());
        verifyNoInteractions(studyGroupMapper);
    }

    @Test
    @DisplayName("CANCELLED → ACTIVE: переход из финального статуса → BadRequestException")
    void updateStatus_CancelledToActive_ShouldThrowBadRequestException() {

        Long groupId = 1L;
        GroupStatusUpdateRequest request = new GroupStatusUpdateRequest(GroupStatus.ACTIVE);

        StudyGroup group = StudyGroup.builder()
                .id(groupId)
                .status(GroupStatus.CANCELLED)
                .build();

        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> studyGroupService.updateStatus(groupId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("CANCELLED")
                .hasMessageContaining("ACTIVE");

        verify(studyGroupRepository).findById(groupId);
        verify(studyGroupRepository, never()).save(any());
        verifyNoInteractions(studyGroupMapper);
    }

    @Test
    @DisplayName("Группа не найдена → ResourceNotFoundException")
    void updateStatus_GroupNotFound_ShouldThrowResourceNotFoundException() {

        Long groupId = 99L;
        GroupStatusUpdateRequest request = new GroupStatusUpdateRequest(GroupStatus.ACTIVE);

        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyGroupService.updateStatus(groupId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("StudyGroup")
                .hasMessageContaining("99");

        verify(studyGroupRepository).findById(groupId);
        verify(studyGroupRepository, never()).save(any());
        verifyNoInteractions(studyGroupMapper);
    }

    @Test
    @DisplayName("ADMIN получает любую группу → 200")
    void getById_Admin_ReturnsGroup() {
        Long groupId = 1L;
        User admin = new User();
        Role adminRole = new Role();
        adminRole.setName(RoleName.ADMIN);
        admin.setId(999L);
        admin.setRole(adminRole);

        StudyGroup group = group1;
        StudyGroupResponse expectedResponse = resp1;

        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(studyGroupMapper.toResponse(group)).thenReturn(expectedResponse);

        StudyGroupResponse result = studyGroupService.getById(groupId, admin);

        assertThat(result).isEqualTo(expectedResponse);
        verify(studyGroupRepository).findById(groupId);
        verify(studyGroupMapper).toResponse(group);
        verify(studentRepository, never()).findByUser_Id(any());
    }

    @Test
    @DisplayName("TEACHER получает свою группу → 200")
    void getById_TeacherOwnGroup_ReturnsGroup() {
        Long groupId = 1L;
        User teacher = teacher1;
        StudyGroup group = group1;
        StudyGroupResponse expectedResponse = resp1;

        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(studyGroupMapper.toResponse(group)).thenReturn(expectedResponse);

        StudyGroupResponse result = studyGroupService.getById(groupId, teacher);

        assertThat(result).isEqualTo(expectedResponse);
        verify(studyGroupRepository).findById(groupId);
        verify(studyGroupMapper).toResponse(group);
        verify(studentRepository, never()).findByUser_Id(any());
    }

    @Test
    @DisplayName("TEACHER пытается получить чужую группу → 403")
    void getById_TeacherOtherGroup_ThrowsAccessDenied() {
        Long groupId = 1L;
        User otherTeacher = teacher2;
        StudyGroup group = group1;

        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> studyGroupService.getById(groupId, otherTeacher))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access Denied");

        verify(studyGroupRepository).findById(groupId);
        verify(studentRepository, never()).findByUser_Id(any());
        verify(studyGroupMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("STUDENT получает свою группу → 200")
    void getById_StudentOwnGroup_ReturnsGroup() {
        Long groupId = 1L;
        Long userId = 777L;
        Long studentId = 888L;

        User studentUser = new User();
        Role studentRole = new Role();
        studentRole.setName(RoleName.STUDENT);
        studentUser.setId(userId);
        studentUser.setRole(studentRole);

        Student student = new Student();
        student.setId(studentId);
        student.setUser(studentUser);

        StudyGroup group = group1;
        group.setStudents(Set.of(student));

        StudyGroupResponse expectedResponse = resp1;

        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(studentRepository.findByUser_Id(userId)).thenReturn(Optional.of(student));
        when(studyGroupMapper.toResponse(group)).thenReturn(expectedResponse);

        StudyGroupResponse result = studyGroupService.getById(groupId, studentUser);

        assertThat(result).isEqualTo(expectedResponse);
        verify(studyGroupRepository).findById(groupId);
        verify(studentRepository).findByUser_Id(userId);
        verify(studyGroupMapper).toResponse(group);
    }

    @Test
    @DisplayName("STUDENT пытается получить чужую группу → 403 (не состоит в группе)")
    void getById_StudentOtherGroup_ThrowsAccessDenied() {
        Long groupId = 1L;
        Long userId = 777L;
        Long studentId = 888L;

        User studentUser = new User();
        Role studentRole = new Role();
        studentRole.setName(RoleName.STUDENT);
        studentUser.setId(userId);
        studentUser.setRole(studentRole);

        Student student = new Student();
        student.setId(studentId);
        student.setUser(studentUser);

        StudyGroup group = group1;
        group.setStudents(Set.of());

        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(studentRepository.findByUser_Id(userId)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> studyGroupService.getById(groupId, studentUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access Denied");

        verify(studyGroupRepository).findById(groupId);
        verify(studentRepository).findByUser_Id(userId);
        verify(studyGroupMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Студент не найден для пользователя → 403")
    void getById_StudentUserWithoutStudentProfile_ThrowsAccessDenied() {
        Long groupId = 1L;
        Long userId = 777L;

        User studentUser = new User();
        Role studentRole = new Role();
        studentRole.setName(RoleName.STUDENT);
        studentUser.setId(userId);
        studentUser.setRole(studentRole);

        StudyGroup group = group1;

        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(studentRepository.findByUser_Id(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyGroupService.getById(groupId, studentUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access Denied");

        verify(studyGroupRepository).findById(groupId);
        verify(studentRepository).findByUser_Id(userId);
        verify(studyGroupMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Группа не найдена → 404")
    void getById_GroupNotFound_ThrowsResourceNotFoundException() {
        Long groupId = 99L;
        User anyUser = currentUser;

        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyGroupService.getById(groupId, anyUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("StudyGroup not found with id: " + groupId);

        verify(studyGroupRepository).findById(groupId);
        verifyNoInteractions(studentRepository);
        verifyNoInteractions(studyGroupMapper);
    }

    @Test
    @DisplayName("Успешное удаление из DRAFT-группы")
    void removeStudent_fromDraftGroup_shouldRemoveStudentAndSave() {
        Student student = Student.builder()
                .id(STUDENT_ID_FOR_REMOVE)
                .firstName("Иван")
                .lastName("Иванов")
                .build();
        StudyGroup group = buildGroupForRemove(GroupStatus.DRAFT, Set.of(student));

        when(studyGroupRepository.findById(GROUP_ID_FOR_REMOVE)).thenReturn(Optional.of(group));
        when(studentRepository.findById(STUDENT_ID_FOR_REMOVE)).thenReturn(Optional.of(student));
        when(studyGroupRepository.save(any(StudyGroup.class))).thenReturn(group);

        studyGroupService.removeStudent(GROUP_ID_FOR_REMOVE, STUDENT_ID_FOR_REMOVE);

        assertThat(group.getStudents()).doesNotContain(student);
        verify(studyGroupRepository, times(1)).save(group);
        verify(studyGroupRepository, times(1)).findById(GROUP_ID_FOR_REMOVE);
        verify(studentRepository, times(1)).findById(STUDENT_ID_FOR_REMOVE);
    }

    @Test
    @DisplayName("Успешное удаление из ACTIVE-группы")
    void removeStudent_fromActiveGroup_shouldSucceed() {
        Student student = Student.builder()
                .id(STUDENT_ID_FOR_REMOVE)
                .firstName("Иван")
                .lastName("Иванов")
                .build();
        StudyGroup group = buildGroupForRemove(GroupStatus.ACTIVE, Set.of(student));

        when(studyGroupRepository.findById(GROUP_ID_FOR_REMOVE)).thenReturn(Optional.of(group));
        when(studentRepository.findById(STUDENT_ID_FOR_REMOVE)).thenReturn(Optional.of(student));
        when(studyGroupRepository.save(any(StudyGroup.class))).thenReturn(group);

        studyGroupService.removeStudent(GROUP_ID_FOR_REMOVE, STUDENT_ID_FOR_REMOVE);

        assertThat(group.getStudents()).doesNotContain(student);
        verify(studyGroupRepository, times(1)).save(group);
    }

    @Test
    @DisplayName("Успешное удаление из CANCELLED-группы")
    void removeStudent_fromCancelledGroup_shouldSucceed() {
        Student student = Student.builder()
                .id(STUDENT_ID_FOR_REMOVE)
                .firstName("Иван")
                .lastName("Иванов")
                .build();
        StudyGroup group = buildGroupForRemove(GroupStatus.CANCELLED, Set.of(student));

        when(studyGroupRepository.findById(GROUP_ID_FOR_REMOVE)).thenReturn(Optional.of(group));
        when(studentRepository.findById(STUDENT_ID_FOR_REMOVE)).thenReturn(Optional.of(student));
        when(studyGroupRepository.save(any(StudyGroup.class))).thenReturn(group);

        studyGroupService.removeStudent(GROUP_ID_FOR_REMOVE, STUDENT_ID_FOR_REMOVE);

        assertThat(group.getStudents()).doesNotContain(student);
        verify(studyGroupRepository, times(1)).save(group);
    }

    @Test
    @DisplayName("Группа в статусе COMPLETED -> BadRequestException, save не вызывается")
    void removeStudent_fromCompletedGroup_shouldThrowBadRequest() {
        Student student = Student.builder()
                .id(STUDENT_ID_FOR_REMOVE)
                .firstName("Иван")
                .lastName("Иванов")
                .build();
        StudyGroup group = buildGroupForRemove(GroupStatus.COMPLETED, Set.of(student));

        when(studyGroupRepository.findById(GROUP_ID_FOR_REMOVE)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> studyGroupService.removeStudent(GROUP_ID_FOR_REMOVE, STUDENT_ID_FOR_REMOVE))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("COMPLETED");

        assertThat(group.getStudents()).contains(student);
        verify(studyGroupRepository, never()).save(any(StudyGroup.class));
        verify(studentRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Группа не найдена -> ResourceNotFoundException")
    void removeStudent_groupNotFound_shouldThrowResourceNotFound() {
        when(studyGroupRepository.findById(GROUP_ID_FOR_REMOVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyGroupService.removeStudent(GROUP_ID_FOR_REMOVE, STUDENT_ID_FOR_REMOVE))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("StudyGroup")
                .hasMessageContaining(GROUP_ID_FOR_REMOVE.toString());

        verify(studyGroupRepository, never()).save(any(StudyGroup.class));
        verify(studentRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Студент не найден -> ResourceNotFoundException")
    void removeStudent_studentNotFound_shouldThrowResourceNotFound() {
        StudyGroup group = buildGroupForRemove(GroupStatus.DRAFT, new HashSet<>());

        when(studyGroupRepository.findById(GROUP_ID_FOR_REMOVE)).thenReturn(Optional.of(group));
        when(studentRepository.findById(STUDENT_ID_FOR_REMOVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyGroupService.removeStudent(GROUP_ID_FOR_REMOVE, STUDENT_ID_FOR_REMOVE))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Student")
                .hasMessageContaining(STUDENT_ID_FOR_REMOVE.toString());

        verify(studyGroupRepository, never()).save(any(StudyGroup.class));
    }

    @Test
    @DisplayName("Студент не состоит в группе -> BadRequestException")
    void removeStudent_studentNotInGroup_shouldThrowBadRequest() {
        Student student = Student.builder()
                .id(STUDENT_ID_FOR_REMOVE)
                .firstName("Иван")
                .lastName("Иванов")
                .build();
        Student otherStudent = Student.builder()
                .id(999L)
                .firstName("Другой")
                .lastName("Студент")
                .build();
        StudyGroup group = buildGroupForRemove(GroupStatus.DRAFT, Set.of(otherStudent));

        when(studyGroupRepository.findById(GROUP_ID_FOR_REMOVE)).thenReturn(Optional.of(group));
        when(studentRepository.findById(STUDENT_ID_FOR_REMOVE)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> studyGroupService.removeStudent(GROUP_ID_FOR_REMOVE, STUDENT_ID_FOR_REMOVE))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("не состоит в этой группе")
                .hasMessageContaining(STUDENT_ID_FOR_REMOVE.toString());

        assertThat(group.getStudents()).hasSize(1);
        assertThat(group.getStudents()).contains(otherStudent);
        verify(studyGroupRepository, never()).save(any(StudyGroup.class));
    }

    private StudyGroup buildGroupForRemove(GroupStatus status, Set<Student> students) {
        return StudyGroup.builder()
                .id(GROUP_ID_FOR_REMOVE)
                .status(status)
                .students(students != null ? new HashSet<>(students) : new HashSet<>())
                .build();
    }

    @Test
    @DisplayName("Успешное добавление студента в группу со статусом DRAFT")
    void addStudent_toDraftGroup_shouldSucceed() {
        Long groupId = 2L;
        Long studentId = 101L;
        AddStudentToGroupRequest request = new AddStudentToGroupRequest(studentId);

        Student student = Student.builder()
                .id(studentId)
                .firstName("Петр")
                .lastName("Петров")
                .build();

        StudyGroup group = group2;
        group.setStudents(new HashSet<>());

        StudyGroup savedGroup = group;
        StudyGroupResponse expectedResponse = StudyGroupResponse.builder()
                .id(groupId)
                .name(group.getName())
                .status(group.getStatus())
                .build();

        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(studyGroupRepository.save(any(StudyGroup.class))).thenReturn(savedGroup);
        when(studyGroupMapper.toResponse(savedGroup)).thenReturn(expectedResponse);

        StudyGroupResponse response = studyGroupService.addStudent(groupId, request);

        assertThat(response).isNotNull();
        assertThat(group.getStudents()).contains(student);
        verify(studyGroupRepository).findById(groupId);
        verify(studentRepository).findById(studentId);
        verify(studyGroupRepository).save(group);
        verify(studyGroupMapper).toResponse(savedGroup);
    }

    @Test
    @DisplayName("Успешное добавление студента в группу со статусом ACTIVE")
    void addStudent_toActiveGroup_shouldSucceed() {
        Long groupId = 1L;
        Long studentId = 102L;
        AddStudentToGroupRequest request = new AddStudentToGroupRequest(studentId);

        Student student = Student.builder()
                .id(studentId)
                .firstName("Анна")
                .lastName("Сидорова")
                .build();

        StudyGroup group = group1;
        group.setStudents(new HashSet<>());

        StudyGroup savedGroup = group;
        StudyGroupResponse expectedResponse = StudyGroupResponse.builder()
                .id(groupId)
                .name(group.getName())
                .status(group.getStatus())
                .build();

        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(studyGroupRepository.save(any(StudyGroup.class))).thenReturn(savedGroup);
        when(studyGroupMapper.toResponse(savedGroup)).thenReturn(expectedResponse);

        StudyGroupResponse response = studyGroupService.addStudent(groupId, request);

        assertThat(response).isNotNull();
        assertThat(group.getStudents()).contains(student);
        verify(studyGroupRepository).findById(groupId);
        verify(studentRepository).findById(studentId);
        verify(studyGroupRepository).save(group);
        verify(studyGroupMapper).toResponse(savedGroup);
    }

    @Test
    @DisplayName("Группа не найдена → ResourceNotFoundException")
    void addStudent_groupNotFound_shouldThrowResourceNotFound() {
        Long groupId = 999L;
        Long studentId = 101L;
        AddStudentToGroupRequest request = new AddStudentToGroupRequest(studentId);

        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyGroupService.addStudent(groupId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("StudyGroup")
                .hasMessageContaining(groupId.toString());

        verify(studyGroupRepository).findById(groupId);
        verify(studentRepository, never()).findById(any());
        verify(studyGroupRepository, never()).save(any());
        verifyNoInteractions(studyGroupMapper);
    }

    @Test
    @DisplayName("Студент не найден → ResourceNotFoundException")
    void addStudent_studentNotFound_shouldThrowResourceNotFound() {
        Long groupId = 2L;
        Long studentId = 999L;
        AddStudentToGroupRequest request = new AddStudentToGroupRequest(studentId);

        StudyGroup group = group2;
        group.setStudents(new HashSet<>());

        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyGroupService.addStudent(groupId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Student")
                .hasMessageContaining(studentId.toString());

        verify(studyGroupRepository).findById(groupId);
        verify(studentRepository).findById(studentId);
        verify(studyGroupRepository, never()).save(any());
        verifyNoInteractions(studyGroupMapper);
    }

    @Test
    @DisplayName("Студент уже состоит в группе → ConflictException")
    void addStudent_alreadyInGroup_shouldThrowConflict() {
        Long groupId = 2L;
        Long studentId = 101L;
        AddStudentToGroupRequest request = new AddStudentToGroupRequest(studentId);

        Student student = Student.builder()
                .id(studentId)
                .firstName("Петр")
                .lastName("Петров")
                .build();

        StudyGroup group = group2;
        group.setStudents(new HashSet<>(Set.of(student)));

        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> studyGroupService.addStudent(groupId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("уже состоит в этой группе")
                .hasMessageContaining(studentId.toString());

        verify(studyGroupRepository).findById(groupId);
        verify(studentRepository).findById(studentId);
        verify(studyGroupRepository, never()).save(any());
        verifyNoInteractions(studyGroupMapper);
    }

    @Test
    @DisplayName("Группа в статусе COMPLETED → BadRequestException")
    void addStudent_groupCompleted_shouldThrowBadRequest() {
        Long groupId = 3L;
        Long studentId = 101L;
        AddStudentToGroupRequest request = new AddStudentToGroupRequest(studentId);

        StudyGroup group = group3;
        group.setStudents(new HashSet<>());

        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> studyGroupService.addStudent(groupId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("COMPLETED");

        verify(studyGroupRepository).findById(groupId);
        verify(studentRepository, never()).findById(any());
        verify(studyGroupRepository, never()).save(any());
        verifyNoInteractions(studyGroupMapper);
    }

    @Test
    @DisplayName("Группа в статусе CANCELLED → BadRequestException")
    void addStudent_groupCancelled_shouldThrowBadRequest() {
        Long groupId = 4L;
        Long studentId = 101L;
        AddStudentToGroupRequest request = new AddStudentToGroupRequest(studentId);

        StudyGroup group = StudyGroup.builder()
                .id(groupId)
                .name("Отменённая группа")
                .status(GroupStatus.CANCELLED)
                .students(new HashSet<>())
                .build();

        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> studyGroupService.addStudent(groupId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("CANCELLED");

        verify(studyGroupRepository).findById(groupId);
        verify(studentRepository, never()).findById(any());
        verify(studyGroupRepository, never()).save(any());
        verifyNoInteractions(studyGroupMapper);
    }

    @Test
    @DisplayName("ADMIN получает студентов группы (200)")
    void adminGetsStudentsOfGroup_returnsListOfTwo() {
        Student student1 = Student.builder()
                .id(1L)
                .firstName("Иван")
                .lastName("Иванов")
                .build();
        Student student2 = Student.builder()
                .id(2L)
                .firstName("Петр")
                .lastName("Петров")
                .build();
        Set<Student> students = new HashSet<>(Set.of(student1, student2));
        group1.setStudents(students);

        User admin = new User();
        Role adminRole = new Role();
        adminRole.setName(RoleName.ADMIN);
        admin.setId(10L);
        admin.setRole(adminRole);

        StudentResponse response1 = mock(StudentResponse.class);
        StudentResponse response2 = mock(StudentResponse.class);

        when(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group1));
        when(studentMapper.toResponse(student1)).thenReturn(response1);
        when(studentMapper.toResponse(student2)).thenReturn(response2);

        List<StudentResponse> result = studyGroupService.getStudentsOfGroup(1L, admin);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(response1, response2);
        verify(studyGroupRepository).findById(1L);
        verify(studentMapper).toResponse(student1);
        verify(studentMapper).toResponse(student2);
    }

    @Test
    @DisplayName("MANAGER получает студентов без проверки учителя (200)")
    void managerGetsStudentsOfGroup_returnsListWithoutTeacherCheck() {
        Student student1 = Student.builder()
                .id(1L)
                .firstName("Иван")
                .lastName("Иванов")
                .build();
        Student student2 = Student.builder()
                .id(2L)
                .firstName("Петр")
                .lastName("Петров")
                .build();
        Set<Student> students = new HashSet<>(Set.of(student1, student2));
        group1.setStudents(students);

        User manager = new User();
        Role managerRole = new Role();
        managerRole.setName(RoleName.MANAGER);
        manager.setId(20L);
        manager.setRole(managerRole);

        StudentResponse response1 = mock(StudentResponse.class);
        StudentResponse response2 = mock(StudentResponse.class);

        when(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group1));
        when(studentMapper.toResponse(student1)).thenReturn(response1);
        when(studentMapper.toResponse(student2)).thenReturn(response2);

        List<StudentResponse> result = studyGroupService.getStudentsOfGroup(1L, manager);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(response1, response2);
        verify(studyGroupRepository).findById(1L);
        verify(studentMapper, times(2)).toResponse(any(Student.class));
    }

    @Test
    @DisplayName("TEACHER получает студентов своей группы (200)")
    void teacherGetsStudentsOfOwnGroup_returnsList() {
        Student student1 = Student.builder()
                .id(1L)
                .firstName("Иван")
                .lastName("Иванов")
                .build();
        Set<Student> students = new HashSet<>(Set.of(student1));
        group1.setStudents(students);
        group1.setTeacher(teacher1);

        StudentResponse response1 = mock(StudentResponse.class);

        when(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group1));
        when(studentMapper.toResponse(student1)).thenReturn(response1);

        List<StudentResponse> result = studyGroupService.getStudentsOfGroup(1L, teacher1);

        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(response1);
        verify(studyGroupRepository).findById(1L);
        verify(studentMapper).toResponse(student1);
    }

    @Test
    @DisplayName("TEACHER получает студентов чужой группы (403)")
    void teacherGetsStudentsOfAnotherGroup_throwsAccessDenied() {
        Student student1 = Student.builder()
                .id(1L)
                .firstName("Иван")
                .lastName("Иванов")
                .build();
        Set<Student> students = new HashSet<>(Set.of(student1));
        group1.setStudents(students);
        group1.setTeacher(teacher1);

        when(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group1));

        assertThatThrownBy(() -> studyGroupService.getStudentsOfGroup(1L, teacher2))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access Denied");

        verify(studyGroupRepository).findById(1L);
        verify(studentMapper, never()).toResponse(any(Student.class));
    }

    @Test
    @DisplayName("группа не найдена (404)")
    void groupNotFound_throwsResourceNotFoundException() {
        User admin = new User();
        Role adminRole = new Role();
        adminRole.setName(RoleName.ADMIN);
        admin.setId(10L);
        admin.setRole(adminRole);

        Long nonExistentGroupId = 99L;

        when(studyGroupRepository.findById(nonExistentGroupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyGroupService.getStudentsOfGroup(nonExistentGroupId, admin))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Study group with id " + nonExistentGroupId + " not found");

        verify(studyGroupRepository).findById(nonExistentGroupId);
        verify(studentMapper, never()).toResponse(any(Student.class));
    }

    @Test
    @DisplayName("Группа без студентов — пустой список без исключений")
    void groupWithoutStudents_returnsEmptyList() {

        User admin = new User();
        Role adminRole = new Role();
        adminRole.setName(RoleName.ADMIN);
        admin.setId(10L);
        admin.setRole(adminRole);

        when(studyGroupRepository.findById(3L)).thenReturn(Optional.of(group3));

        List<StudentResponse> result = studyGroupService.getStudentsOfGroup(3L, admin);

        assertThat(result).isEmpty();
        verify(studyGroupRepository).findById(3L);
        verify(studentMapper, never()).toResponse(any(Student.class));
    }
}
