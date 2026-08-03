package com.covenantcode.crm.controller;

import com.covenantcode.crm.BaseIntegrationTest;
import com.covenantcode.crm.dto.teacher.TeacherCreateRequest;
import com.covenantcode.crm.dto.teacher.TeacherUpdateRequest;
import com.covenantcode.crm.dto.user.EnabledUpdateRequest;
import com.covenantcode.crm.entity.Course;
import com.covenantcode.crm.entity.Lesson;
import com.covenantcode.crm.entity.Role;
import com.covenantcode.crm.entity.StudyGroup;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.CourseStatus;
import com.covenantcode.crm.entity.enums.GroupStatus;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.repository.CourseRepository;
import com.covenantcode.crm.repository.LessonRepository;
import com.covenantcode.crm.repository.RoleRepository;
import com.covenantcode.crm.repository.StudyGroupRepository;
import com.covenantcode.crm.repository.UserRepository;
import com.covenantcode.crm.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("TeacherController Integration Tests")
class TeacherControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StudyGroupRepository studyGroupRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private LessonRepository lessonRepository;

    private String adminToken;
    private User testTeacher;
    private String teacherToken;
    private String otherTeacherToken;
    private String studentToken;
    private User otherTeacher;

    @BeforeEach
    void setUp() {

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ADMIN).build()));
        Role teacherRole = roleRepository.findByName(RoleName.TEACHER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.TEACHER).build()));
        Role studentRole = roleRepository.findByName(RoleName.STUDENT)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.STUDENT).build()));

        User admin = User.builder()
                .firstName("Admin")
                .lastName("Adminov")
                .email("admin@test.com")
                .password(passwordEncoder.encode("admin123"))
                .phone("+79161234560")
                .enabled(true)
                .role(adminRole)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        userRepository.save(admin);

        testTeacher = User.builder()
                .firstName("Иван")
                .lastName("Петров")
                .email("ivan.petrov@school.ru")
                .password(passwordEncoder.encode("teacher123"))
                .phone("+79161234567")
                .enabled(true)
                .role(teacherRole)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        userRepository.save(testTeacher);

        otherTeacher = User.builder()
                .firstName("Сергей").lastName("Сидоров").email("sergey.sidorov@school.ru")
                .password(passwordEncoder.encode("teacher123")).phone("+79161234568")
                .enabled(true).role(teacherRole)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC)).updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        userRepository.save(otherTeacher);

        User student = User.builder()
                .firstName("Петр").lastName("Иванов").email("student@test.com")
                .password(passwordEncoder.encode("student123")).phone("+79161234569")
                .enabled(true).role(studentRole)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC)).updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        userRepository.save(student);

        adminToken = jwtService.generateToken(admin);
        teacherToken = jwtService.generateToken(testTeacher);
        otherTeacherToken = jwtService.generateToken(otherTeacher);
        studentToken = jwtService.generateToken(student);

        // 4. Создание инфраструктуры для занятий (Course & StudyGroup)
        Course course = Course.builder()
                .title("Java-разработчик")
                .description("Основы языка Java")
                .durationInWeeks(12)
                .price(new java.math.BigDecimal("50000.00"))
                .status(CourseStatus.ACTIVE)
                .build();
        course = courseRepository.save(course);

        StudyGroup studyGroup = StudyGroup.builder()
                .name("Java-группа июнь 2026")
                .course(course)
                .teacher(testTeacher) // Исправлено: Передаем обязательное поле teacher_id
                .startDate(LocalDate.of(2026, 6, 1)) // Исправлено: Передаем обязательное поле start_date
                .status(GroupStatus.ACTIVE) // Исправлено: Передаем обязательное поле status
                .build();
        studyGroup = studyGroupRepository.save(studyGroup);

        // 5. Сохранение тестовых занятий (пункт 5.1 ТЗ)
        lessonRepository.save(Lesson.builder()
                .teacher(testTeacher)
                .studyGroup(studyGroup)
                .topic("Введение в Java")
                .lessonDate(LocalDate.of(2026, 6, 2))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 30))
                .build());

        lessonRepository.save(Lesson.builder()
                .teacher(testTeacher)
                .studyGroup(studyGroup)
                .topic("Основы Java")
                .lessonDate(LocalDate.of(2026, 6, 3))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 30))
                .build());
    }

    @Test
    @DisplayName("GET /api/v1/teachers — возвращает 200 и список преподавателей")
    void getAllTeachers_shouldReturn200AndTeacherList() throws Exception {
        mockMvc.perform(get("/api/v1/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[*].email")
                        .value(hasItem("ivan.petrov@school.ru")))
                .andExpect(jsonPath("$.content[*].firstName")
                        .value(hasItem("Иван")))
                .andExpect(jsonPath("$.content[*].lastName")
                        .value(hasItem("Петров")))
                .andExpect(jsonPath("$.content[*].enabled")
                        .value(hasItem(true)));
    }

    @Test
    @DisplayName("GET /api/v1/teachers?search=имя — возвращает только отфильтрованных преподавателей")
    void getAllTeachers_withSearch_shouldReturnFilteredTeachers() throws Exception {
        String search = "Анна";

        User secondTeacher = User.builder()
                .firstName("Анна")
                .lastName("Смирнова")
                .email("anna.smirnova@school.ru")
                .password(passwordEncoder.encode("teacher123"))
                .phone("+79161234568")
                .enabled(true)
                .role(roleRepository.findByName(RoleName.TEACHER).orElseThrow())
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        userRepository.save(secondTeacher);

        mockMvc.perform(get("/api/v1/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", search)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content").value(hasSize(1)))
                .andExpect(jsonPath("$.content[0].firstName").value("Анна"))
                .andExpect(jsonPath("$.content[0].lastName").value("Смирнова"))
                .andExpect(jsonPath("$.content[0].email").value("anna.smirnova@school.ru"))
                .andExpect(jsonPath("$.content[*].firstName").value(not(hasItem("Иван"))))
                .andExpect(jsonPath("$.content[*].lastName").value(not(hasItem("Петров"))));
    }

    @Test
    @DisplayName("GET /api/v1/teachers с токеном STUDENT — возвращает 403 Forbidden")
    void getAllTeachers_withStudentToken_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/teachers")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("forbidden"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("POST /api/v1/teachers — 201 Created, преподаватель сохранён")
    @Transactional
    void createTeacher_shouldReturn201AndSaveTeacher() throws Exception {
        String uniqueEmail = "new.teacher." + System.currentTimeMillis() + "@school.ru";

        TeacherCreateRequest request = TeacherCreateRequest.builder()
                .firstName("Пётр")
                .lastName("Сидоров")
                .email(uniqueEmail)
                .password("securePass123")
                .phone("+79161234599")
                .build();

        mockMvc.perform(post("/api/v1/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value(uniqueEmail))
                .andExpect(jsonPath("$.firstName").value("Пётр"))
                .andExpect(jsonPath("$.lastName").value("Сидоров"))
                .andExpect(jsonPath("$.phone").value("+79161234599"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.createdAt").exists());

        User saved = userRepository.findByEmail(uniqueEmail).orElseThrow();
        assertThat(saved.getFirstName()).isEqualTo("Пётр");
        assertThat(saved.getRole().getName()).isEqualTo(RoleName.TEACHER);
        assertThat(saved.getPassword()).isNotEqualTo("securePass123");
    }

    @Test
    @DisplayName("POST /api/v1/teachers — 409 Conflict при повторном email")
    void createTeacher_shouldReturn409_whenEmailDuplicate() throws Exception {
        TeacherCreateRequest request = TeacherCreateRequest.builder()
                .firstName("Дубликат")
                .lastName("Иванов")
                .email(testTeacher.getEmail())
                .password("anotherPass123")
                .phone("+79161234588")
                .build();

        mockMvc.perform(post("/api/v1/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("conflict"))
                .andExpect(jsonPath("$.detail").value("Пользователь с email " + testTeacher.getEmail() + " уже существует"));
    }

    @Test
    @DisplayName("POST /api/v1/teachers — 403 Forbidden для пользователя без роли ADMIN")
    void createTeacher_shouldReturn403_whenNotAdmin() throws Exception {
        String teacherToken = jwtService.generateToken(testTeacher);

        TeacherCreateRequest request = TeacherCreateRequest.builder()
                .firstName("Нелегальный")
                .lastName("Учитель")
                .email("illegal@school.ru")
                .password("somePass123")
                .build();

        mockMvc.perform(post("/api/v1/teachers")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("forbidden"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("POST /api/v1/teachers — 400 Bad Request при невалидных данных")
    void createTeacher_shouldReturn400_whenValidationFails() throws Exception {
        TeacherCreateRequest invalidRequest = TeacherCreateRequest.builder()
                .firstName("")
                .lastName("Тестов")
                .email("not-an-email")
                .password("short")
                .phone("+79161234567")
                .build();

        mockMvc.perform(post("/api/v1/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("validation-error"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(3))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("firstName")))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("email")))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("password")));
    }

    @Test
    @DisplayName("DELETE /api/v1/teachers/{id} — 204 без групп, преподаватель удалён")
    void deleteTeacher_shouldReturn204_whenNoGroups() throws Exception {
        lessonRepository.deleteAllInBatch();
        studyGroupRepository.deleteAllInBatch();

        Long teacherId = testTeacher.getId();

        mockMvc.perform(delete("/api/v1/teachers/{id}", teacherId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(teacherId)).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/v1/teachers/{id} — 409 Conflict, если есть группы")
    @Transactional
    void deleteTeacher_shouldReturn409_whenHasGroups() throws Exception {
        Long teacherId = testTeacher.getId();

        Course course = Course.builder()
                .title("Математика")
                .description("Базовый курс")
                .durationInWeeks(12)
                .price(new BigDecimal("1000.00"))
                .status(CourseStatus.ACTIVE)
                .build();
        course = courseRepository.save(course);

        StudyGroup group = StudyGroup.builder()
                .name("Группа А-101")
                .course(course)
                .teacher(testTeacher)
                .startDate(LocalDate.now())
                .status(GroupStatus.ACTIVE)
                .build();
        studyGroupRepository.save(group);

        mockMvc.perform(delete("/api/v1/teachers/{id}", teacherId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("conflict"))
                .andExpect(jsonPath("$.detail").value(
                        matchesPattern("Невозможно удалить преподавателя: у него \\d+ групп\\(ы\\)\\. Сначала переназначьте группы\\.")
                ));

        assertThat(userRepository.findById(teacherId)).isPresent();
    }

    @Test
    @DisplayName("DELETE /api/v1/teachers/{id} — 404 Not Found для несуществующего ID")
    void deleteTeacher_shouldReturn404_whenTeacherNotFound() throws Exception {
        Long nonExistentId = 9999L;

        mockMvc.perform(delete("/api/v1/teachers/{id}", nonExistentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("resource-not-found"))
                .andExpect(jsonPath("$.detail").value(
                        "Преподаватель с id " + nonExistentId + " не найден"
                ));
    }

    @Test
    @DisplayName("DELETE /api/v1/teachers/{id} — 403 Forbidden для не-ADMIN")
    void deleteTeacher_shouldReturn403_whenNotAdmin() throws Exception {
        Long teacherId = testTeacher.getId();
        String teacherToken = jwtService.generateToken(testTeacher);

        mockMvc.perform(delete("/api/v1/teachers/{id}", teacherId)
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("forbidden"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("GET /api/v1/teachers/{id} - возвращает 200 для существующего преподавателя")
    void getTeacherById_shouldReturn200ForExistingTeacher() throws Exception {
        mockMvc.perform(get("/api/v1/teachers/{id}", testTeacher.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testTeacher.getId()))
                .andExpect(jsonPath("$.firstName").value(testTeacher.getFirstName()))
                .andExpect(jsonPath("$.lastName").value(testTeacher.getLastName()))
                .andExpect(jsonPath("$.email").value(testTeacher.getEmail()));
    }

    @Test
    @DisplayName("GET /api/v1/teachers/{id} — несуществующий ID возвращает 404")
    void getTeacherById_whenNotExists_returns404() throws Exception {
        Long nonExistentId = 999L;

        mockMvc.perform(get("/api/v1/teachers/{id}", nonExistentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("resource-not-found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Преподаватель не найден"));
    }

    @Test
    @DisplayName("GET /api/v1/teachers/{id} — возвращает 404 для пользователя, который не является преподавателем")
    void getTeacherById_shouldReturn404ForNonTeacherUser() throws Exception {
        Role studentRole = roleRepository.findByName(RoleName.STUDENT)
                .orElseGet(() -> {
                    Role newRole = Role.builder()
                            .name(RoleName.STUDENT)
                            .build();
                    return roleRepository.save(newRole);
                });

        User student = User.builder()
                .firstName("Student")
                .lastName("Studentov")
                .email("student@example.com")
                .password(passwordEncoder.encode("student123"))
                .phone("+79161234569")
                .enabled(true)
                .role(studentRole)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        userRepository.save(student);

        mockMvc.perform(get("/api/v1/teachers/{id}", student.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.type").value("resource-not-found"))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value(
                        "Преподаватель не найден"
                ))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("GET /api/v1/teachers/{id} — возвращает 403 при использовании токена STUDENT")
    void getTeacherById_withStudentToken_shouldReturn403() throws Exception {
        Role studentRole = roleRepository.findByName(RoleName.STUDENT)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.STUDENT).build()));

        User testStudent = User.builder()
                .firstName("Student")
                .lastName("Studentov")
                .email("student_only_for_this_test@test.com")
                .password(passwordEncoder.encode("student123"))
                .role(studentRole)
                .build();

        User savedStudent = userRepository.save(testStudent);

        String studentToken = jwtService.generateToken(savedStudent);

        mockMvc.perform(get("/api/v1/teachers/{id}", testTeacher.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/teachers/{id} — 200, данные обновились")
    void updateTeacher_ShouldReturn200AndUpdateData() throws Exception {

        Long teacherId = testTeacher.getId();
        TeacherUpdateRequest request = TeacherUpdateRequest.builder()
                .firstName("Алексей")
                .lastName("Смирнов")
                .phone("+79169999999")
                .build();

        mockMvc.perform(put("/api/v1/teachers/{id}", teacherId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Алексей"))
                .andExpect(jsonPath("$.lastName").value("Смирнов"))
                .andExpect(jsonPath("$.phone").value("+79169999999"));

        User updatedTeacher = userRepository.findById(teacherId).orElseThrow();
        assertThat(updatedTeacher.getFirstName()).isEqualTo("Алексей");
        assertThat(updatedTeacher.getLastName()).isEqualTo("Смирнов");
        assertThat(updatedTeacher.getPhone()).isEqualTo("+79169999999");
    }

    @Test
    @DisplayName("PATCH /api/v1/teachers/{id}/enabled — 200, enabled изменился")
    void setEnabled_ShouldReturn200AndEnabledChanged() throws Exception {

        Long teacherId = testTeacher.getId();
        EnabledUpdateRequest request = new EnabledUpdateRequest();
        request.setEnabled(false);

        mockMvc.perform(patch("/api/v1/teachers/{id}/enabled", teacherId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        User blockedTeacher = userRepository.findById(teacherId).orElseThrow();
        assertThat(blockedTeacher.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("PUT /api/v1/teachers/{id} с несуществующим ID — 404")
    void updateTeacher_NotFound_Returns404() throws Exception {

        Long nonExistentId = 9999L;
        TeacherUpdateRequest request = TeacherUpdateRequest.builder()
                .firstName("Алексей")
                .lastName("Смирнов")
                .phone("+79169999999")
                .build();

        mockMvc.perform(put("/api/v1/teachers/{id}", nonExistentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("resource-not-found"))
                .andExpect(jsonPath("$.detail").value("Преподаватель с id " + nonExistentId + " не найден"));
    }

    @Test
    @DisplayName("PUT /api/v1/teachers/{id} не-ADMIN — 403")
    void updateTeacher_NotAdmin_Returns403() throws Exception {

        Long teacherId = testTeacher.getId();
        String teacherToken = jwtService.generateToken(testTeacher);

        TeacherUpdateRequest request = TeacherUpdateRequest.builder()
                .firstName("Алексей")
                .lastName("Смирнов")
                .phone("+79169999999")
                .build();

        mockMvc.perform(put("/api/v1/teachers/{id}", teacherId)
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("forbidden"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("PATCH /api/v1/teachers/{id}/enabled не-ADMIN — 403")
    void setEnabled_NotAdmin_Returns403() throws Exception {
        Long teacherId = testTeacher.getId();
        String teacherToken = jwtService.generateToken(testTeacher);

        EnabledUpdateRequest request = new EnabledUpdateRequest();
        request.setEnabled(false);

        mockMvc.perform(patch("/api/v1/teachers/{id}/enabled", teacherId)
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("forbidden"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Тест 1: ADMIN запрашивает расписание любого преподавателя — успех")
    void adminCanGetAnyTeacherSchedule_ShouldReturn200AndSortedList() throws Exception {
        mockMvc.perform(get("/api/v1/teachers/{teacherId}/lessons", testTeacher.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                // Проверяем сортировку: lessonDate ASC, startTime ASC согласно ТЗ
                .andExpect(jsonPath("$[0].lessonDate").value("2026-06-02"))
                .andExpect(jsonPath("$[0].startTime").value("18:00:00"))
                .andExpect(jsonPath("$[1].lessonDate").value("2026-06-03"))
                .andExpect(jsonPath("$[1].startTime").value("10:00:00"));
    }

    @Test
    @DisplayName("Тест 2: TEACHER запрашивает своё расписание с фильтром по датам → HTTP 200")
    void teacherCanGetOwnScheduleWithDateFilter_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/teachers/{teacherId}/lessons", testTeacher.getId())
                        .header("Authorization", "Bearer " + teacherToken)
                        .param("dateFrom", "2026-06-01")
                        .param("dateTo", "2026-06-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                // По фильтру подходит только первое занятие (до 2026-06-02 включительно)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].lessonDate").value("2026-06-02"))
                .andExpect(jsonPath("$[0].topic").value("Введение в Java"));
    }

    @Test
    @DisplayName("Тест 3: TEACHER запрашивает чужое расписание → HTTP 403")
    void teacherCannotGetOtherTeacherSchedule_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/teachers/{teacherId}/lessons", otherTeacher.getId())
                        .header("Authorization", "Bearer " + teacherToken)) // Токен Ивана, а ID в URL Сергея
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Тест 4: несуществующий teacherId → HTTP 404")
    void nonexistentTeacherId_ShouldReturn404() throws Exception {
        Long nonexistentId = 999L;
        mockMvc.perform(get("/api/v1/teachers/{teacherId}/lessons", nonexistentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                // Валидация структуры глобальной ошибки по ТЗ
                .andExpect(jsonPath("$.type").value("resource-not-found"))
                .andExpect(jsonPath("$.detail").value("User с id " + nonexistentId + " не найден"));
    }

    @Test
    @DisplayName("Тест 5: роль STUDENT → HTTP 403")
    void studentRoleCannotAccessEndpoint_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/teachers/{teacherId}/lessons", testTeacher.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }
}
