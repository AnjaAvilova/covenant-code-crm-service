package com.covenantcode.crm.controller;

import com.covenantcode.crm.BaseIntegrationTest;
import com.covenantcode.crm.dto.course.CourseCreateRequest;
import com.covenantcode.crm.dto.course.CourseUpdateRequest;
import com.covenantcode.crm.entity.Course;
import com.covenantcode.crm.entity.Role;
import com.covenantcode.crm.entity.StudyGroup;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.CourseStatus;
import com.covenantcode.crm.entity.enums.GroupStatus;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.repository.RoleRepository;
import com.covenantcode.crm.repository.StudyGroupRepository;
import com.covenantcode.crm.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import com.covenantcode.crm.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;


@AutoConfigureMockMvc
public class CourseControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyGroupRepository studyGroupRepository;

    @BeforeEach
    void setUp() {

        studyGroupRepository.deleteAll();
        courseRepository.deleteAll();

        Course javaCourse = new Course();
        javaCourse.setTitle("Java Backend");
        javaCourse.setStatus(CourseStatus.ACTIVE);
        javaCourse.setPrice(BigDecimal.valueOf(10000));
        javaCourse.setDurationInWeeks(12);
        courseRepository.save(javaCourse);

        Course pythonCourse = new Course();
        pythonCourse.setTitle("English Speaking");
        pythonCourse.setStatus(CourseStatus.ACTIVE);
        pythonCourse.setPrice(BigDecimal.valueOf(5000));
        pythonCourse.setDurationInWeeks(8);
        pythonCourse.setDescription("Description for English");

        courseRepository.save(pythonCourse);
    }

    @AfterEach
    void tearDown() {
        studyGroupRepository.deleteAll();
        courseRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void createCourse_success_returns201() throws Exception {
        CourseCreateRequest request = new CourseCreateRequest();
        request.setTitle("Java для начинающих");
        request.setDescription("Полный курс по Java");
        request.setDurationInWeeks(16);
        request.setPrice(new BigDecimal("45000.00"));
        request.setStatus(CourseStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Java для начинающих"))
                .andExpect(jsonPath("$.durationInWeeks").value(16))
                .andExpect(jsonPath("$.price").value(45000.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void createCourse_statusNotProvided_statusActiveInResponse() throws Exception {
        CourseCreateRequest request = new CourseCreateRequest();
        request.setTitle("Бесплатный курс");
        request.setDurationInWeeks(8);
        request.setPrice(BigDecimal.ZERO);
        request.setStatus(null);

        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void createCourse_negativeDuration_returns400() throws Exception {
        CourseCreateRequest request = new CourseCreateRequest();
        request.setTitle("Некорректный курс");
        request.setDurationInWeeks(-5);
        request.setPrice(new BigDecimal("10000.00"));

        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("validation-error"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void createCourse_negativePrice_returns400() throws Exception {
        CourseCreateRequest request = new CourseCreateRequest();
        request.setTitle("Курс с отрицательной ценой");
        request.setDurationInWeeks(10);
        request.setPrice(new BigDecimal("-500.00"));

        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("validation-error"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void createCourse_zeroDuration_returns400() throws Exception {
        CourseCreateRequest request = new CourseCreateRequest();
        request.setTitle("Курс с нулевой длительностью");
        request.setDurationInWeeks(0);
        request.setPrice(new BigDecimal("10000.00"));

        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("validation-error"));
    }

    @Test
    @DisplayName("POST /api/v1/courses - TEACHER → 403 Forbidden")
    @WithMockUser(roles = "TEACHER")
    void createCourse_withTeacherRole_returns403() throws Exception {
        CourseCreateRequest request = new CourseCreateRequest();
        request.setTitle("Тестовый курс");
        request.setDescription("Описание");
        request.setDurationInWeeks(10);
        request.setPrice(new BigDecimal("10000.00"));

        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getById_WhenCourseExists_ShouldReturnOk() throws Exception {

        Course course = new Course();
        course.setTitle("Spring Boot Master");
        course.setDescription("Advanced course");
        course.setPrice(BigDecimal.valueOf(100.0));
        course.setDurationInWeeks(8);
        course.setStatus(CourseStatus.ACTIVE);

        Course savedCourse = courseRepository.save(course);

        mockMvc.perform(get("/api/v1/courses/{id}", savedCourse.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(savedCourse.getId()))
                .andExpect(jsonPath("$.title").value("Spring Boot Master"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getById_WhenCourseDoesNotExist_ShouldReturnNotFound() throws Exception {

        mockMvc.perform(get("/api/v1/courses/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("resource-not-found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getById_WhenUnauthorized_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/courses/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/v1/courses/{id} - ADMIN успешно удаляет курс → 204 No Content")
    @WithMockUser(roles = "ADMIN")
    public void deleteCourse_success_returns204() throws Exception {
        CourseCreateRequest createRequest = new CourseCreateRequest();
        createRequest.setTitle("Курс для удаления");
        createRequest.setDescription("Этот курс будет удален");
        createRequest.setDurationInWeeks(12);
        createRequest.setPrice(new BigDecimal("25000.00"));
        createRequest.setStatus(CourseStatus.ACTIVE);

        Course course = new Course();
        course.setTitle(createRequest.getTitle());
        course.setDescription(createRequest.getDescription());
        course.setDurationInWeeks(createRequest.getDurationInWeeks());
        course.setPrice(createRequest.getPrice());
        course.setStatus(CourseStatus.ACTIVE);

        Course savedCourse = courseRepository.save(course);
        Long courseId = savedCourse.getId();

        mockMvc.perform(delete("/api/v1/courses/{id}", courseId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        assertTrue(courseRepository.findById(courseId).isEmpty(),
                "Курс должен быть удален из базы данных");
    }

    @Test
    @DisplayName("DELETE /api/v1/courses/{id} - курс с активными группами → 409 Conflict")
    @WithMockUser(roles = "ADMIN")
    public void deleteCourse_withActiveGroups_returns409() throws Exception {
        Course course = new Course();
        course.setTitle("Курс с активными группами");
        course.setDescription("Этот курс имеет активные учебные группы");
        course.setDurationInWeeks(16);
        course.setPrice(new BigDecimal("35000.00"));
        course.setStatus(CourseStatus.ACTIVE);
        Course savedCourse = courseRepository.save(course);
        Long courseId = savedCourse.getId();

        Optional<Role> existingRole = roleRepository.findByName(RoleName.TEACHER);
        Role teacherRole;
        if (existingRole.isPresent()) {
            teacherRole = existingRole.get();
        } else {
            teacherRole = Role.builder()
                    .name(RoleName.TEACHER)
                    .build();
            teacherRole = roleRepository.save(teacherRole);
        }

        User teacher = User.builder()
                .firstName("Иван")
                .lastName("Петров")
                .email("teacher_activetest@example.com")
                .password("password123")
                .role(teacherRole)
                .enabled(true)
                .build();
        User savedTeacher = userRepository.save(teacher);

        StudyGroup studyGroup = StudyGroup.builder()
                .name("Группа Java-16")
                .course(savedCourse)
                .teacher(savedTeacher)
                .startDate(LocalDate.now())
                .status(GroupStatus.ACTIVE)
                .students(new HashSet<>())
                .build();
        studyGroupRepository.save(studyGroup);

        mockMvc.perform(delete("/api/v1/courses/{id}", courseId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("conflict"))
                .andExpect(jsonPath("$.detail").value("Невозможно удалить курс: существуют активные учебные группы"));

        assertTrue(courseRepository.findById(courseId).isPresent(),
                "Курс должен оставаться в базе данных");
    }

    @Test
    @DisplayName("DELETE /api/v1/courses/{id} - курс не найден → 404 Not Found")
    @WithMockUser(roles = "ADMIN")
    public void deleteCourse_notFound_returns404() throws Exception {
        Long nonExistentCourseId = 99999L;

        mockMvc.perform(delete("/api/v1/courses/{id}", nonExistentCourseId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("resource-not-found"));
    }

    @Test
    @DisplayName("DELETE /api/v1/courses/{id} - MANAGER пытается удалить курс → 403 Forbidden")
    @WithMockUser(roles = "MANAGER")
    public void deleteCourse_withManagerRole_returns403() throws Exception {
        Course course = new Course();
        course.setTitle("Курс для проверки доступа менеджера");
        course.setDescription("Менеджер не должен удалять курс");
        course.setDurationInWeeks(10);
        course.setPrice(new BigDecimal("15000.00"));
        course.setStatus(CourseStatus.ACTIVE);
        Course savedCourse = courseRepository.save(course);

        mockMvc.perform(delete("/api/v1/courses/{id}", savedCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void update_shouldReturn200AndUpdateCourse() throws Exception {
        Course course = Course.builder()
                .title("Old title")
                .description("Old description")
                .durationInWeeks(8)
                .price(new BigDecimal("99.99"))
                .status(CourseStatus.ACTIVE)
                .build();

        course = courseRepository.save(course);

        CourseUpdateRequest request = new CourseUpdateRequest(
                "New title",
                "New description",
                12,
                new BigDecimal("199.99"),
                CourseStatus.ACTIVE
        );

        mockMvc.perform(put("/api/v1/courses/{id}", course.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New title"))
                .andExpect(jsonPath("$.description").value("New description"))
                .andExpect(jsonPath("$.durationInWeeks").value(12))
                .andExpect(jsonPath("$.price").value(199.99))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        Course updated = courseRepository.findById(course.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("New title");
        assertThat(updated.getDescription()).isEqualTo("New description");
        assertThat(updated.getDurationInWeeks()).isEqualTo(12);
        assertThat(updated.getPrice()).isEqualByComparingTo("199.99");
        assertThat(updated.getStatus()).isEqualTo(CourseStatus.ACTIVE);
    }

    @Test
    @WithMockUser(username = "manager", roles = {"MANAGER"})
    void update_shouldReturn200ForManagerRole() throws Exception {
        Course course = Course.builder()
                .title("Old title")
                .description("Old description")
                .durationInWeeks(8)
                .price(new BigDecimal("99.99"))
                .status(CourseStatus.ACTIVE)
                .build();

        course = courseRepository.save(course);

        CourseUpdateRequest request = new CourseUpdateRequest(
                "Updated title",
                "Updated description",
                10,
                new BigDecimal("149.99"),
                CourseStatus.ACTIVE
        );

        mockMvc.perform(put("/api/v1/courses/{id}", course.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void update_shouldReturn404_whenCourseNotFound() throws Exception {
        CourseUpdateRequest request = new CourseUpdateRequest(
                "New title",
                "New description",
                12,
                new BigDecimal("199.99"),
                CourseStatus.ACTIVE
        );

        mockMvc.perform(put("/api/v1/courses/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void update_shouldReturn400_whenValidationFails() throws Exception {
        CourseUpdateRequest request = new CourseUpdateRequest(
                "",
                "desc",
                -1,
                new BigDecimal("-10"),
                null
        );

        mockMvc.perform(put("/api/v1/courses/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void update_shouldReturn403_forTeacherRole() throws Exception {
        CourseUpdateRequest request = new CourseUpdateRequest(
                "title", "desc", 8, new BigDecimal("100"), CourseStatus.ACTIVE
        );
        mockMvc.perform(put("/api/v1/courses/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/courses - без фильтра - 200 OK с пагинацией")
    @WithMockUser(roles = "MANAGER")
    void getAll_withoutFilter_returns200WithAllCourses() throws Exception {

        courseRepository.deleteAll();
        courseRepository.save(buildCourse("Java", CourseStatus.ACTIVE));
        courseRepository.save(buildCourse("English", CourseStatus.ARCHIVED));
        courseRepository.save(buildCourse("Python", CourseStatus.ACTIVE));

        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/courses - фильтр status=ACTIVE - только активные")
    @WithMockUser(roles = "MANAGER")
    void getAll_withActiveFilter_returnsOnlyActive() throws Exception {

        courseRepository.deleteAll();

        courseRepository.save(buildCourse("Java", CourseStatus.ACTIVE));
        courseRepository.save(buildCourse("English", CourseStatus.ARCHIVED));

        mockMvc.perform(get("/api/v1/courses")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("фильтр status=ARCHIVED — только архивные")
    @WithMockUser(roles = "MANAGER")
    void getAll_withArchivedFilter_returnsOnlyArchived() throws Exception {
        courseRepository.save(buildCourse("Java", CourseStatus.ACTIVE));
        courseRepository.save(buildCourse("English", CourseStatus.ARCHIVED));

        mockMvc.perform(get("/api/v1/courses")
                        .param("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("ARCHIVED"));
    }

    @Test
    @DisplayName("тест 401")
    void getAll_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isUnauthorized());
    }

    private Course buildCourse(String title, CourseStatus status) {
        Course course = new Course();
        course.setTitle(title);
        course.setDescription("desc");
        course.setDurationInWeeks(4);
        course.setPrice(BigDecimal.valueOf(1000));
        course.setStatus(status);
        return course;
    }

    @Test
    @DisplayName("Поиск по title — находит курс")
    @WithMockUser(roles = "USER")
    void shouldFindCourseByTitle() throws Exception {
        mockMvc.perform(get("/api/v1/courses")
                        .param("search", "java")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title", containsString("Java")));
    }

    @Test
    @DisplayName("Поиск с search и status, без совпадений — пустой результат")
    @WithMockUser(roles = "USER")
    void shouldReturnEmptyWhenSearchAndStatusDoNotMatch() throws Exception {
        mockMvc.perform(get("/api/v1/courses")
                        .param("search", "java")
                        .param("status", "ARCHIVED")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Запрос без параметров — возвращает все курсы")
    @WithMockUser(roles = "USER")
    void shouldReturnAllCoursesWithoutParams() throws Exception {
        mockMvc.perform(get("/api/v1/courses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("Запрос без токена — 401 Unauthorized")
    void shouldReturnUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/courses")
                        .param("search", "java"))
                .andExpect(status().isUnauthorized());
    }
}
