package com.covenantcode.crm.controller;

import com.covenantcode.crm.BaseIntegrationTest;
import com.covenantcode.crm.dto.lesson.LessonUpdateRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class LessonControllerIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private StudyGroupRepository studyGroupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    // Поля для сущностей (БЕЗ @Autowired)
    private Course testCourse;
    private StudyGroup testGroup1;
    private StudyGroup testGroup2;
    private StudyGroup anotherGroup;
    private User testAdmin;
    private User testManager;
    private User testTeacher;
    private User anotherTeacher;

    private String adminToken;
    private String managerToken;
    private String teacherToken;

    private final String baseUrl = "/api/v1/lessons";

    // Вспомогательный метод для генерации заголовка авторизации
    private String bearer(String token) {
        return "Bearer " + token;
    }

    @BeforeEach
    void setUp() {
        // 1. Сначала создаем и сохраняем обязательные роли
        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setName(RoleName.ADMIN);
                    return roleRepository.save(r);
                });

        Role managerRole = roleRepository.findByName(RoleName.MANAGER)
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setName(RoleName.MANAGER);
                    return roleRepository.save(r);
                });

        Role teacherRole = roleRepository.findByName(RoleName.TEACHER)
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setName(RoleName.TEACHER);
                    return roleRepository.save(r);
                });

        // 2. Создаем и сохраняем пользователей
        testAdmin = new User();
        testAdmin.setFirstName("Admin");
        testAdmin.setLastName("Test");
        testAdmin.setEmail("admin@test.com");
        testAdmin.setPassword(passwordEncoder.encode("password"));
        testAdmin.setRole(adminRole);
        testAdmin.setEnabled(true);
        testAdmin = userRepository.save(testAdmin);

        testManager = new User();
        testManager.setFirstName("Manager");
        testManager.setLastName("Test");
        testManager.setEmail("manager@test.com");
        testManager.setPassword(passwordEncoder.encode("password"));
        testManager.setRole(managerRole);
        testManager.setEnabled(true);
        testManager = userRepository.save(testManager);

        testTeacher = new User();
        testTeacher.setFirstName("Teacher");
        testTeacher.setLastName("Test");
        testTeacher.setEmail("teacher@test.com");
        testTeacher.setPassword(passwordEncoder.encode("password"));
        testTeacher.setRole(teacherRole);
        testTeacher.setEnabled(true);
        testTeacher = userRepository.save(testTeacher);

        // Добавляем второго преподавателя для проверки изоляции данных
        anotherTeacher = new User();
        anotherTeacher.setFirstName("Another");
        anotherTeacher.setLastName("Teacher");
        anotherTeacher.setEmail("another.teacher@test.com");
        anotherTeacher.setPassword(passwordEncoder.encode("password"));
        anotherTeacher.setRole(teacherRole);
        anotherTeacher.setEnabled(true);
        anotherTeacher = userRepository.save(anotherTeacher);

        // 3. Создаем и сохраняем обязательный курс
        testCourse = new Course();
        testCourse.setTitle("Test Course");
        testCourse.setDescription("Test Description");
        testCourse.setPrice(BigDecimal.valueOf(150.00));
        testCourse.setDurationInWeeks(6);
        testCourse.setStatus(CourseStatus.ACTIVE);
        testCourse = courseRepository.save(testCourse);

        // 4. Создаем и сохраняем учебные группы со ВСЕМИ обязательными полями
        testGroup1 = new StudyGroup();
        testGroup1.setName("Java Core 2026");
        testGroup1.setCourse(testCourse);
        testGroup1.setTeacher(testTeacher);
        testGroup1.setStartDate(LocalDate.of(2026, 6, 1));
        testGroup1.setStatus(GroupStatus.ACTIVE);
        testGroup1 = studyGroupRepository.save(testGroup1);

        testGroup2 = new StudyGroup();
        testGroup2.setName("Spring Advanced 2026");
        testGroup2.setCourse(testCourse);
        testGroup2.setTeacher(testTeacher);
        testGroup2.setStartDate(LocalDate.of(2026, 6, 1));
        testGroup2.setStatus(GroupStatus.ACTIVE);
        testGroup2 = studyGroupRepository.save(testGroup2);

        // Группа для второго преподавателя
        anotherGroup = new StudyGroup();
        anotherGroup.setName("Python Basic 2026");
        anotherGroup.setCourse(testCourse);
        anotherGroup.setTeacher(anotherTeacher);
        anotherGroup.setStartDate(LocalDate.of(2026, 6, 1));
        anotherGroup.setStatus(GroupStatus.ACTIVE);
        anotherGroup = studyGroupRepository.save(anotherGroup);

        // Генерируем валидные JWT-токены
        adminToken = jwtService.generateToken(testAdmin);
        managerToken = jwtService.generateToken(testManager);
        teacherToken = jwtService.generateToken(testTeacher);
    }

    @Test
    @DisplayName("Тест 1: без фильтров — все занятия")
    void getAll_WithoutFilters_ReturnsAllLessons() throws Exception {
        // Given
        lessonRepository.save(createLesson(testGroup1, testTeacher, LocalDate.of(2026, 6, 1)));
        lessonRepository.save(createLesson(testGroup2, testTeacher, LocalDate.of(2026, 6, 15)));
        lessonRepository.save(createLesson(testGroup1, testTeacher, LocalDate.of(2026, 7, 1)));

        // When & Then
        mockMvc.perform(get(baseUrl)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @DisplayName("Тест 2: фильтр по groupId — только занятия нужной группы")
    void getAll_WithGroupIdFilter_ReturnsFilteredLessons() throws Exception {
        // Given
        lessonRepository.save(createLesson(testGroup1, testTeacher, LocalDate.of(2026, 6, 1)));
        lessonRepository.save(createLesson(testGroup1, testTeacher, LocalDate.of(2026, 6, 15)));
        lessonRepository.save(createLesson(testGroup2, testTeacher, LocalDate.of(2026, 6, 20)));

        // When & Then
        mockMvc.perform(get(baseUrl)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                        .param("groupId", testGroup1.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("Тест 3: фильтр по диапазону дат")
    void getAll_WithDateRangeFilter_ReturnsLessonsInBounds() throws Exception {
        // Given
        lessonRepository.save(createLesson(testGroup1, testTeacher, LocalDate.of(2026, 6, 1)));  // Входит
        lessonRepository.save(createLesson(testGroup1, testTeacher, LocalDate.of(2026, 6, 15))); // Входит
        lessonRepository.save(createLesson(testGroup1, testTeacher, LocalDate.of(2026, 7, 1)));  // Не входит

        // When & Then
        mockMvc.perform(get(baseUrl)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("dateFrom", "2026-06-01")
                        .param("dateTo", "2026-06-30")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("Тест 4: преподаватель получает HTTP 200 и видит только свои занятия")
    void getAll_AsTeacher_ReturnsOnlyOwnLessons() throws Exception {
        // Given
        lessonRepository.save(createLesson(testGroup1, testTeacher, LocalDate.of(2026, 6, 1)));   // Свое
        lessonRepository.save(createLesson(testGroup2, testTeacher, LocalDate.of(2026, 6, 15)));  // Свое
        lessonRepository.save(createLesson(anotherGroup, anotherTeacher, LocalDate.of(2026, 6, 20))); // Чужое

        // When & Then
        mockMvc.perform(get(baseUrl)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2)) // Должно вернуться только 2 занятия из 3
                .andExpect(jsonPath("$.content[0].teacherId").value(testTeacher.getId().intValue()))
                .andExpect(jsonPath("$.content[1].teacherId").value(testTeacher.getId().intValue()));
    }

    private Lesson createLesson(StudyGroup group, User teacher, LocalDate date) {
        return Lesson.builder()
                .studyGroup(group)
                .teacher(teacher)
                .topic("Тестовая тема занятия")
                .lessonDate(date)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 30))
                .build();
    }

    @Test
    @DisplayName("Тест 1: успешное обновление → HTTP 200, поля изменились")
    void update_Success_Returns200AndUpdatedFields() throws Exception {
        // Given - Создаем и сохраняем исходное занятие в БД
        Lesson initialLesson = createLesson(testGroup1, testTeacher, LocalDate.of(2026, 9, 1));
        initialLesson = lessonRepository.save(initialLesson);

        // Готовим запрос на изменение группы (на testGroup2), топика и времени
        LessonUpdateRequest request = new LessonUpdateRequest();
        request.setGroupId(testGroup2.getId());
        request.setTeacherId(testTeacher.getId());
        request.setTopic("Новая измененная тема занятия");
        request.setDescription("Обновленное описание");
        request.setLessonDate(LocalDate.of(2026, 9, 2));
        request.setStartTime(LocalTime.of(14, 0));
        request.setEndTime(LocalTime.of(16, 0));

        // When & Then
        mockMvc.perform(put(baseUrl + "/{id}", initialLesson.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(initialLesson.getId()))
                // ИСПРАВЛЕНО: проверяем $.studyGroupId вместо $.groupId
                .andExpect(jsonPath("$.studyGroupId").value(testGroup2.getId()))
                .andExpect(jsonPath("$.teacherId").value(testTeacher.getId()))
                .andExpect(jsonPath("$.topic").value("Новая измененная тема занятия"));
    }

    @Test
    @DisplayName("Тест 2: группа в статусе COMPLETED → HTTP 400")
    void update_GroupCompleted_Returns400() throws Exception {
        // Given - Создаем и переводим группу в COMPLETED
        StudyGroup completedGroup = new StudyGroup();
        completedGroup.setName("Архивная группа");
        completedGroup.setCourse(testCourse);
        completedGroup.setTeacher(testTeacher);
        completedGroup.setStartDate(LocalDate.of(2025, 1, 1));
        completedGroup.setStatus(GroupStatus.COMPLETED);
        completedGroup = studyGroupRepository.save(completedGroup);

        // Создаем занятие, привязанное к завершенной группе
        Lesson initialLesson = createLesson(completedGroup, testTeacher, LocalDate.of(2025, 1, 15));
        initialLesson = lessonRepository.save(initialLesson);

        LessonUpdateRequest request = new LessonUpdateRequest();
        request.setGroupId(testGroup1.getId()); // Пытаемся перепривязать к активной, но сама сущность из COMPLETED
        request.setTeacherId(testTeacher.getId());
        request.setTopic("Попытка редактирования");
        request.setLessonDate(LocalDate.of(2026, 9, 1));
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(11, 0));

        // When & Then
        mockMvc.perform(put(baseUrl + "/{id}", initialLesson.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Тест 3: занятие не найдено → HTTP 404")
    void update_LessonNotFound_Returns404() throws Exception {
        // Given
        Long nonExistingId = 9999L;
        LessonUpdateRequest request = new LessonUpdateRequest();
        request.setGroupId(testGroup1.getId());
        request.setTeacherId(testTeacher.getId());
        request.setTopic("Тема для несуществующего занятия");
        request.setLessonDate(LocalDate.of(2026, 9, 1));
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(11, 0));

        // When & Then
        mockMvc.perform(put(baseUrl + "/{id}", nonExistingId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Тест 4: пересечение занятий → HTTP 409")
    void update_TeacherOverlap_Returns409() throws Exception {
        // Given - Создаем два занятия в БД (одно будем обновлять, второе — это чужое занятие, создающее конфликт)
        Lesson targetLesson = createLesson(testGroup1, testTeacher, LocalDate.of(2026, 9, 1));
        targetLesson.setStartTime(LocalTime.of(9, 0));
        targetLesson.setEndTime(LocalTime.of(11, 0));
        targetLesson = lessonRepository.save(targetLesson);

        Lesson conflictingLesson = createLesson(testGroup2, testTeacher, LocalDate.of(2026, 9, 1));
        conflictingLesson.setStartTime(LocalTime.of(14, 0));
        conflictingLesson.setEndTime(LocalTime.of(16, 0));
        lessonRepository.save(conflictingLesson);

        // Пытаемся обновить время targetLesson (09:00) так, чтобы оно наложилось на чужое (14:00)
        LessonUpdateRequest request = new LessonUpdateRequest();
        request.setGroupId(testGroup1.getId());
        request.setTeacherId(testTeacher.getId());
        request.setTopic("Сдвигаем время на конфликтное");
        request.setLessonDate(LocalDate.of(2026, 9, 1));
        request.setStartTime(LocalTime.of(13, 30)); // Наложение на 14:00-16:00
        request.setEndTime(LocalTime.of(15, 0));

        // When & Then
        mockMvc.perform(put(baseUrl + "/{id}", targetLesson.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Тест 5: роль TEACHER → HTTP 403")
    void update_RoleTeacher_Returns403Forbidden() throws Exception {
        // Given
        Lesson initialLesson = createLesson(testGroup1, testTeacher, LocalDate.of(2026, 9, 1));
        initialLesson = lessonRepository.save(initialLesson);

        LessonUpdateRequest request = new LessonUpdateRequest();
        request.setGroupId(testGroup1.getId());
        request.setTeacherId(testTeacher.getId());
        request.setTopic("Преподаватель пытается отредактировать");
        request.setLessonDate(LocalDate.of(2026, 9, 1));
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(11, 0));

        // When & Then
        // Spring Security заблокирует этот запрос на уровне @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
        mockMvc.perform(put(baseUrl + "/{id}", initialLesson.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(teacherToken)) // Токен TEACHER
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

}
