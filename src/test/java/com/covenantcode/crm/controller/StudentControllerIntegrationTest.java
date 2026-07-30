package com.covenantcode.crm.controller;

import com.covenantcode.crm.BaseIntegrationTest;
import com.covenantcode.crm.dto.student.StudentCreateRequest;
import com.covenantcode.crm.dto.student.StudentUpdateRequest;
import com.covenantcode.crm.entity.Course;
import com.covenantcode.crm.entity.Role;
import com.covenantcode.crm.entity.Student;
import com.covenantcode.crm.entity.StudyGroup;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.CourseStatus;
import com.covenantcode.crm.entity.enums.GroupStatus;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.repository.CourseRepository;
import com.covenantcode.crm.repository.RoleRepository;
import com.covenantcode.crm.repository.StudentRepository;
import com.covenantcode.crm.repository.StudyGroupRepository;
import com.covenantcode.crm.repository.UserRepository;
import com.covenantcode.crm.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentControllerIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudyGroupRepository studyGroupRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User adminUser;
    private User teacherUser;
    private User studentUser;
    private User otherStudentUser;

    private Student student;
    private Course course;
    private StudyGroup studyGroup;

    private String adminToken;
    private String teacherToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        // Создаём все необходимые роли
        createRolesIfNotExists();

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new RuntimeException("ADMIN role not found"));
        Role teacherRole = roleRepository.findByName(RoleName.TEACHER)
                .orElseThrow(() -> new RuntimeException("TEACHER role not found"));
        Role studentRole = roleRepository.findByName(RoleName.STUDENT)
                .orElseThrow(() -> new RuntimeException("STUDENT role not found"));

        adminUser = userRepository.save(User.builder()
                .firstName("Admin44").lastName("User33").email("adminwwwest@test.ru")
                .password(passwordEncoder.encode("password123")).role(adminRole).enabled(true).build());

        teacherUser = userRepository.save(User.builder()
                .firstName("Teacher").lastName("User").email("teacher@test.ru")
                .password(passwordEncoder.encode("password123")).role(teacherRole).enabled(true).build());

        studentUser = userRepository.save(User.builder()
                .firstName("Student").lastName("One").email("student1@test.ru")
                .password(passwordEncoder.encode("password123"))
                .role(studentRole)
                .enabled(true)
                .build());

        otherStudentUser = userRepository.save(User.builder()
                .firstName("Other").lastName("Student").email("other@test.ru")
                .password(passwordEncoder.encode("password123")).role(studentRole).enabled(true).build());

        course = courseRepository.save(Course.builder()
                .title("Java Developer")
                .description("Description")
                .price(new BigDecimal("10000.00"))
                .durationInWeeks(12)
                .status(CourseStatus.ACTIVE)
                .build());

        student = studentRepository.save(Student.builder()
                .firstName("Иван").lastName("Иванов").email("ivan@test.ru")
                .user(studentUser)
                .build());

        studyGroup = studyGroupRepository.save(StudyGroup.builder()
                .name("Java Backend 101")
                .course(course)
                .teacher(teacherUser)
                .startDate(LocalDate.now().plusDays(10))
                .status(GroupStatus.ACTIVE)
                .students(Set.of(student))
                .build());

        adminToken = jwtService.generateToken(adminUser);
        teacherToken = jwtService.generateToken(teacherUser);
        studentToken = jwtService.generateToken(studentUser);
    }

    private void createRolesIfNotExists() {
        for (RoleName roleName : RoleName.values()) {
            if (!roleRepository.findByName(roleName).isPresent()) {
                Role role = Role.builder().name(roleName).build();
                roleRepository.save(role);
            }
        }
    }

    private Role getOrCreateRole(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
    }

    @Test
    @DisplayName("Тест 1: ADMIN получает любого студента (200)")
    void adminGetsAnyStudent_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/students/{id}", student.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(student.getId()));
    }

    @Test
    @DisplayName("Тест 2: TEACHER получает студента из своей группы (200)")
    void teacherGetsStudentFromHisGroup_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/students/{id}", student.getId())
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Тест 3: TEACHER получает студента из чужой группы (403)")
    void teacherGetsStudentFromAnotherGroup_shouldReturn403() throws Exception {
        User otherTeacher = userRepository.save(User.builder()
                .firstName("Other")
                .lastName("Teacher")
                .email("other-teacher@test.ru")
                .password(passwordEncoder.encode("password123"))
                .role(getOrCreateRole(RoleName.TEACHER))
                .enabled(true).build());
        String otherTeacherToken = jwtService.generateToken(otherTeacher);

        mockMvc.perform(get("/api/v1/students/{id}", student.getId())
                        .header("Authorization", "Bearer " + otherTeacherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Тест 4: STUDENT получает свой профиль (200)")
    void studentGetsOwnProfile_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/students/{id}", student.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(student.getId()));
    }

    @Test
    @DisplayName("Тест 5: STUDENT получает чужой профиль (403)")
    void studentGetsAnotherProfile_shouldReturn403() throws Exception {
        String otherStudentToken = jwtService.generateToken(otherStudentUser);

        mockMvc.perform(get("/api/v1/students/{id}", student.getId())
                        .header("Authorization", "Bearer " + otherStudentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Тест 6: студент не найден (404)")
    void studentNotFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/students/{id}", 9999L)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("resource-not-found"));
    }

    @Test
    @DisplayName("Тест 1: успешное создание студента без привязки к пользователю (роль ADMIN)")
    @WithMockUser(roles = "ADMIN")
    void create_ShouldReturnCreated_WhenValidRequestNoUserId() throws Exception {
        studyGroupRepository.deleteAll();
        studentRepository.deleteAll();

        String email = "ivan-" + System.currentTimeMillis() + "@test.com";

        StudentCreateRequest request = StudentCreateRequest.builder()
                .firstName("Иван")
                .lastName("Иванов")
                .email(email)
                .phone("123456789")
                .birthDate(LocalDate.of(2000, 1, 1))
                .userId(null)
                .build();

        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Иван"))
                .andExpect(jsonPath("$.lastName").value("Иванов"))
                .andExpect(jsonPath("$.email").value(email));

        List<Student> students = studentRepository.findAll();
        assertThat(students).hasSize(1);

        Student savedStudent = students.getFirst();
        assertThat(savedStudent.getFirstName()).isEqualTo("Иван");
        assertThat(savedStudent.getLastName()).isEqualTo("Иванов");
        assertThat(savedStudent.getEmail()).isEqualTo(email);
        assertThat(savedStudent.getPhone()).isEqualTo("123456789");
        assertThat(savedStudent.getBirthDate()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(savedStudent.getUser()).isNull();
    }

    @Test
    @DisplayName("Тест 2: ошибка 404, если указанный userId не существует")
    @WithMockUser(roles = "MANAGER")
    void create_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {
        Long nonExistentUserId = 999L;
        StudentCreateRequest request = StudentCreateRequest.builder()
                .firstName("Петр")
                .lastName("Петров")
                .userId(nonExistentUserId)
                .build();

        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type", is("resource-not-found")));
    }

    @Test
    @DisplayName("Тест 3: ошибка 409, если пользователь уже привязан к другому студенту")
    @WithMockUser(roles = "ADMIN")
    void create_ShouldReturnConflict_WhenUserAlreadyLinked() throws Exception {
        Role studentRole = roleRepository.findByName(RoleName.STUDENT)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.STUDENT).build()));

        User existingUser = userRepository.save(User.builder()
                .firstName("User")
                .lastName("X")
                .email("user-x@test.com")
                .password("password")
                .role(studentRole)
                .enabled(true)
                .build());

        studentRepository.save(Student.builder()
                .firstName("Уже")
                .lastName("Существующий")
                .user(existingUser)
                .build());

        StudentCreateRequest request = StudentCreateRequest.builder()
                .firstName("Новый")
                .lastName("Студент")
                .userId(existingUser.getId())
                .build();

        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type", is("conflict")));
    }

    @Test
    @DisplayName("Тест 4: ошибка 403, если студент создается пользователем с ролью TEACHER")
    @WithMockUser(roles = "TEACHER")
    void create_ShouldReturnForbidden_WhenRoleIsTeacher() throws Exception {
        StudentCreateRequest request = StudentCreateRequest.builder()
                .firstName("Access")
                .lastName("Denied")
                .build();

        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/students — список всех студентов (200)")
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void getAllStudents_WithoutFilters_ShouldReturnAllStudents() throws Exception {
        studyGroupRepository.deleteAll();
        studentRepository.deleteAll();
        userRepository.deleteAll();
        courseRepository.deleteAll();

        Role managerRole = getOrCreateRole(RoleName.MANAGER);

        User manager = User.builder()
                .firstName("Manager")
                .lastName("Test")
                .email("manager@example.com")
                .password("password")
                .role(managerRole)
                .enabled(true)
                .build();
        userRepository.save(manager);

        Student student1 = Student.builder()
                .firstName("Алиса")
                .lastName("Смирнова")
                .phone("+79161234567")
                .email("alice@example.com")
                .birthDate(LocalDate.of(2000, 5, 15))
                .build();

        Student student2 = Student.builder()
                .firstName("Борис")
                .lastName("Иванов")
                .phone("+79161112233")
                .email("boris@example.com")
                .birthDate(LocalDate.of(1999, 8, 20))
                .build();

        Student student3 = Student.builder()
                .firstName("Екатерина")
                .lastName("Петрова")
                .phone("+79169998877")
                .email("ekaterina@example.com")
                .birthDate(LocalDate.of(2001, 3, 10))
                .build();

        studentRepository.saveAll(List.of(student1, student2, student3));

        mockMvc.perform(get("/api/v1/students")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.number", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[*].id", containsInAnyOrder(
                        student1.getId().intValue(),
                        student2.getId().intValue(),
                        student3.getId().intValue()
                )))
                .andExpect(jsonPath("$.content[*].firstName", containsInAnyOrder("Алиса", "Борис", "Екатерина")))
                .andExpect(jsonPath("$.content[*].lastName", containsInAnyOrder("Смирнова", "Иванов", "Петрова")))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists());
    }

    @Test
    @DisplayName("GET /api/v1/students?search=Смир — поиск по частичному имени (200)")
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void getAllStudents_SearchByPartialName_ShouldReturnFilteredStudents() throws Exception {

        Role managerRole = getOrCreateRole(RoleName.MANAGER);

        User manager = User.builder()
                .firstName("Manager")
                .lastName("Test")
                .email("manager@example.com")
                .password("password")
                .role(managerRole)
                .enabled(true)
                .build();
        userRepository.save(manager);

        Student student1 = Student.builder()
                .firstName("Алиса")
                .lastName("Смирнова")
                .phone("+79161234567")
                .email("alice@example.com")
                .birthDate(LocalDate.of(2000, 5, 15))
                .build();

        Student student2 = Student.builder()
                .firstName("Борис")
                .lastName("Смирнов")
                .phone("+79161112233")
                .email("boris@example.com")
                .birthDate(LocalDate.of(1999, 8, 20))
                .build();

        Student student3 = Student.builder()
                .firstName("Екатерина")
                .lastName("Петрова")
                .phone("+79169998877")
                .email("ekaterina@example.com")
                .birthDate(LocalDate.of(2001, 3, 10))
                .build();

        Student student4 = Student.builder()
                .firstName("Дмитрий")
                .lastName("Смирновский")
                .phone("+79165554433")
                .email("dmitry@example.com")
                .birthDate(LocalDate.of(1998, 11, 25))
                .build();

        studentRepository.saveAll(List.of(student1, student2, student3, student4));

        mockMvc.perform(get("/api/v1/students")
                        .param("search", "Смир")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.number", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[*].lastName", containsInAnyOrder("Смирнова", "Смирнов", "Смирновский")))
                .andExpect(jsonPath("$.content[*].firstName", containsInAnyOrder("Алиса", "Борис", "Дмитрий")))
                .andExpect(jsonPath("$.content[*].id", containsInAnyOrder(
                        student1.getId().intValue(),
                        student2.getId().intValue(),
                        student4.getId().intValue()
                )))
                .andExpect(jsonPath("$.content[*].lastName", not(containsString("Петрова"))));
    }

    @Test
    @DisplayName("GET /api/v1/students?search=7916 — поиск по телефону (200)")
    @WithMockUser(username = "manager@example.com", roles = "MANAGER")
    void getAllStudents_SearchByPhone_ShouldReturnFilteredStudents() throws Exception {

        Role managerRole = roleRepository.findByName(RoleName.MANAGER).orElseThrow();
        User manager = User.builder()
                .firstName("Manager")
                .lastName("Test")
                .email("manager@example.com")
                .password("password")
                .role(managerRole)
                .build();
        userRepository.save(manager);

        Student student1 = Student.builder()
                .firstName("Алиса")
                .lastName("Смирнова")
                .phone("+79161234567")
                .email("alice@example.com")
                .birthDate(LocalDate.of(2000, 5, 15))
                .build();

        Student student2 = Student.builder()
                .firstName("Борис")
                .lastName("Иванов")
                .phone("+79261112233")
                .email("boris@example.com")
                .birthDate(LocalDate.of(1999, 8, 20))
                .build();

        Student student3 = Student.builder()
                .firstName("Екатерина")
                .lastName("Петрова")
                .phone("+79169998877")
                .email("ekaterina@example.com")
                .birthDate(LocalDate.of(2001, 3, 10))
                .build();

        studentRepository.saveAll(List.of(student1, student2, student3));

        mockMvc.perform(get("/api/v1/students")
                        .param("search", "7916")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.number", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].phone", containsInAnyOrder("+79161234567", "+79169998877")))
                .andExpect(jsonPath("$.content[*].firstName", containsInAnyOrder("Алиса", "Екатерина")));
    }

    @Test
    @DisplayName("GET /api/v1/students — TEACHER имеет доступ к списку своих студентов")
    @WithMockUser(username = "teacher1@mail.com", roles = "TEACHER")
    void getAllStudents_WithTeacherRole_ShouldReturnOnlyOwnStudents() throws Exception {
        studyGroupRepository.deleteAll();
        studentRepository.deleteAll();
        userRepository.deleteAll();
        courseRepository.deleteAll();

        Course course = Course.builder()
                .title("Java Developer")
                .durationInWeeks(4)
                .price(BigDecimal.valueOf(668887))
                .status(CourseStatus.ACTIVE)
                .build();
        courseRepository.save(course);

        Role teacherRole = roleRepository.findByName(RoleName.TEACHER)
                .orElseThrow(() -> new RuntimeException("TEACHER role not found"));

        User teacher1 = User.builder()
                .firstName("Teacher")
                .lastName("One")
                .email("teacher1@mail.com")
                .password("password")
                .role(teacherRole)
                .enabled(true)
                .build();
        userRepository.save(teacher1);

        User teacher2 = User.builder()
                .firstName("Teacher")
                .lastName("Two")
                .email("teacher2@mail.com")
                .password("password")
                .role(teacherRole)
                .enabled(true)
                .build();
        userRepository.save(teacher2);

        Student ownStudent = Student.builder()
                .firstName("Student")
                .lastName("Own")
                .email("own@student.com")
                .phone("+111111111")
                .build();
        studentRepository.save(ownStudent);

        Student otherStudent = Student.builder()
                .firstName("Student")
                .lastName("Other")
                .email("other@student.com")
                .phone("+222222222")
                .build();
        studentRepository.save(otherStudent);

        StudyGroup ownGroup = StudyGroup.builder()
                .name("Teacher 1 Group")
                .teacher(teacher1)
                .course(course)
                .startDate(LocalDate.now().plusDays(29))
                .status(GroupStatus.ACTIVE)
                .students(new HashSet<>(Set.of(ownStudent)))
                .build();
        studyGroupRepository.save(ownGroup);

        StudyGroup otherGroup = StudyGroup.builder()
                .name("Teacher 2 Group")
                .teacher(teacher2)
                .course(course)
                .startDate(LocalDate.now().plusDays(14))
                .status(GroupStatus.COMPLETED)
                .students(new HashSet<>(Set.of(otherStudent)))
                .build();
        studyGroupRepository.save(otherGroup);

        mockMvc.perform(get("/api/v1/students")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id")
                        .value(hasItem(ownStudent.getId().intValue())))
                .andExpect(jsonPath("$.content[*].id")
                        .value(not(hasItem(otherStudent.getId().intValue()))));
    }

    @Test
    @DisplayName("Тест 5: успешное обновление студента (200)")
    @WithMockUser(roles = "ADMIN")
    void update_ShouldReturnOk_WhenValidRequest() throws Exception {
        Student existingStudent = studentRepository.save(Student.builder()
                .firstName("Иван")
                .lastName("Иванов")
                .phone("123456789")
                .email("old@test.com")
                .birthDate(LocalDate.of(2000, 1, 1))
                .build());

        StudentUpdateRequest updateRequest = StudentUpdateRequest.builder()
                .firstName("Пётр")
                .lastName("Петров")
                .phone("+79998887766")
                .email("new@test.com")
                .birthDate(LocalDate.of(1995, 5, 15))
                .build();

        mockMvc.perform(put("/api/v1/students/{id}", existingStudent.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingStudent.getId()))
                .andExpect(jsonPath("$.firstName").value("Пётр"))
                .andExpect(jsonPath("$.lastName").value("Петров"))
                .andExpect(jsonPath("$.phone").value("+79998887766"))
                .andExpect(jsonPath("$.email").value("new@test.com"))
                .andExpect(jsonPath("$.birthDate").value("1995-05-15"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        Student updated = studentRepository.findById(existingStudent.getId()).orElseThrow();
        assertEquals("Пётр", updated.getFirstName());
        assertEquals("Петров", updated.getLastName());
    }

    @Test
    @DisplayName("Тест 6: студент не найден (404)")
    @WithMockUser(roles = "ADMIN")
    void update_ShouldReturnNotFound_WhenStudentDoesNotExist() throws Exception {
        Long nonExistentId = 999L;
        StudentUpdateRequest updateRequest = StudentUpdateRequest.builder()
                .firstName("Неважно")
                .lastName("Неважно")
                .phone("000")
                .build();

        mockMvc.perform(put("/api/v1/students/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("resource-not-found"));
    }

    @Test
    @DisplayName("Тест 7: ошибка валидации (400) — пустое firstName")
    @WithMockUser(roles = "ADMIN")
    void update_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        Student existingStudent = studentRepository.save(Student.builder()
                .firstName("Valid")
                .lastName("Student")
                .phone("111")
                .build());

        StudentUpdateRequest invalidRequest = StudentUpdateRequest.builder()
                .firstName("")
                .lastName("Петров")
                .phone("+79998887766")
                .build();

        mockMvc.perform(put("/api/v1/students/{id}", existingStudent.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("validation-error"));
    }

    @Test
    @DisplayName("Тест 8: TEACHER не может обновить студента (403)")
    @WithMockUser(roles = "TEACHER")
    void update_ShouldReturnForbidden_WhenRoleIsTeacher() throws Exception {
        Student existingStudent = studentRepository.save(Student.builder()
                .firstName("Any")
                .lastName("Student")
                .phone("000")
                .build());

        StudentUpdateRequest updateRequest = StudentUpdateRequest.builder()
                .firstName("Changed")
                .lastName("Name")
                .phone("111")
                .build();

        mockMvc.perform(put("/api/v1/students/{id}", existingStudent.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Тест 9: STUDENT не может обновить студента (403)")
    @WithMockUser(roles = "STUDENT")
    void update_ShouldReturnForbidden_WhenRoleIsStudent() throws Exception {
        Student existingStudent = studentRepository.save(Student.builder()
                .firstName("Any")
                .lastName("Student")
                .phone("000")
                .build());

        StudentUpdateRequest updateRequest = StudentUpdateRequest.builder()
                .firstName("Changed")
                .lastName("Name")
                .phone("111")
                .build();

        mockMvc.perform(put("/api/v1/students/{id}", existingStudent.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Тест 1: успешное удаление студента (204)")
    void deleteStudent_ShouldReturn204_WhenStudentNotInActiveGroup() throws Exception {
        Student studentToDelete = Student.builder()
                .firstName("Test")
                .lastName("Student")
                .email("test-delete@example.com")
                .phone("+1234567890")
                .birthDate(LocalDate.of(2000, 1, 1))
                .build();
        studentToDelete = studentRepository.save(studentToDelete);

        mockMvc.perform(delete("/api/v1/students/{id}", studentToDelete.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(studentRepository.findById(studentToDelete.getId())).isEmpty();
    }

    @Test
    @DisplayName("Тест 2: студент не найден (404)")
    void deleteStudent_ShouldReturn404_WhenStudentNotFound() throws Exception {
        Long nonExistentId = 9999L;

        mockMvc.perform(delete("/api/v1/students/{id}", nonExistentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type", is("resource-not-found")))
                .andExpect(jsonPath("$.detail", containsString("не найден")));
    }

    @Test
    @DisplayName("Тест 3: студент в активной группе (409)")
    void deleteStudent_ShouldReturn409_WhenStudentInActiveGroup() throws Exception {
        Student studentInGroup = Student.builder()
                .firstName("Group")
                .lastName("Student")
                .email("group-student@example.com")
                .phone("+1234567890")
                .birthDate(LocalDate.of(2000, 1, 1))
                .build();
        studentInGroup = studentRepository.save(studentInGroup);

        StudyGroup activeGroup = StudyGroup.builder()
                .name("Active Group For Delete Test")
                .course(course)
                .teacher(teacherUser)
                .startDate(LocalDate.now().plusDays(5))
                .status(GroupStatus.ACTIVE)
                .students(Set.of(studentInGroup))
                .build();
        studyGroupRepository.save(activeGroup);

        mockMvc.perform(delete("/api/v1/students/{id}", studentInGroup.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type", is("conflict")))
                .andExpect(jsonPath("$.detail", containsString("активной учебной группе")));

        assertThat(studentRepository.findById(studentInGroup.getId())).isPresent();
    }

    @Test
    @DisplayName("Тест 4: MANAGER не может удалить студента (403)")
    void deleteStudent_ShouldReturn403_WhenUserIsManager() throws Exception {
        Role managerRole = getOrCreateRole(RoleName.MANAGER);
        User managerUser = userRepository.save(User.builder()
                .firstName("Manager")
                .lastName("Test")
                .email("manager-test@test.ru")
                .password(passwordEncoder.encode("password123"))
                .role(managerRole)
                .enabled(true)
                .build());
        String managerToken = jwtService.generateToken(managerUser);

        Student studentToDelete = Student.builder()
                .firstName("Manager")
                .lastName("Test")
                .email("manager-delete@example.com")
                .phone("+1234567890")
                .birthDate(LocalDate.of(2000, 1, 1))
                .build();
        studentToDelete = studentRepository.save(studentToDelete);

        mockMvc.perform(delete("/api/v1/students/{id}", studentToDelete.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        assertThat(studentRepository.findById(studentToDelete.getId())).isPresent();
    }

    @Test
    @DisplayName("Тест 5: отсутствует токен авторизации (401)")
    void deleteStudent_ShouldReturn401_WhenNoToken() throws Exception {
        Student studentToDelete = Student.builder()
                .firstName("NoAuth")
                .lastName("Student")
                .email("noauth@example.com")
                .phone("+1234567890")
                .birthDate(LocalDate.of(2000, 1, 1))
                .build();
        studentToDelete = studentRepository.save(studentToDelete);

        mockMvc.perform(delete("/api/v1/students/{id}", studentToDelete.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
