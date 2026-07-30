package com.covenantcode.crm.controller;

import com.covenantcode.crm.BaseIntegrationTest;
import com.covenantcode.crm.dto.group.AddStudentToGroupRequest;
import com.covenantcode.crm.dto.group.GroupStatusUpdateRequest;
import com.covenantcode.crm.dto.group.StudyGroupCreateRequest;
import com.covenantcode.crm.dto.group.StudyGroupUpdateRequest;
import com.covenantcode.crm.entity.Course;
import com.covenantcode.crm.entity.Role;
import com.covenantcode.crm.entity.Student;
import com.covenantcode.crm.entity.StudyGroup;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.GroupStatus;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.repository.CourseRepository;
import com.covenantcode.crm.repository.RoleRepository;
import com.covenantcode.crm.repository.StudentRepository;
import com.covenantcode.crm.repository.StudyGroupRepository;
import com.covenantcode.crm.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudyGroupControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StudyGroupRepository studyGroupRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Course testCourse;
    private User teacher;
    private Student student1;
    private Student student2;
    private User manager;
    private User admin;
    private Role adminRole;
    private User teacher2;
    private StudyGroup group1;
    private StudyGroup group2;
    private StudyGroup group3;

    @BeforeEach
    void setUp() {

        Role teacherRole = roleRepository.findByName(RoleName.TEACHER)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(RoleName.TEACHER);
                    return roleRepository.save(newRole);
                });

        Role managerRole = roleRepository.findByName(RoleName.MANAGER)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(RoleName.MANAGER);
                    return roleRepository.save(newRole);
                });

        adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(RoleName.ADMIN);
                    return roleRepository.save(newRole);
                });

        Role studentRole = roleRepository.findByName(RoleName.STUDENT)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(RoleName.STUDENT);
                    return roleRepository.save(newRole);
                });

        admin = User.builder()
                .email("admin@test.com")
                .password("password")
                .firstName("Admin")
                .lastName("Admin")
                .role(adminRole)
                .build();

        userRepository.save(admin);

        testCourse = courseRepository.save(Course.builder()
                .title("Java for Test")
                .description("Integration test course")
                .durationInWeeks(8)
                .price(java.math.BigDecimal.valueOf(1000))
                .status(com.covenantcode.crm.entity.enums.CourseStatus.ACTIVE)
                .build());

        teacher = userRepository.save(User.builder()
                .firstName("Teacher")
                .lastName("Test")
                .email("teacher@test.com")
                .password("encoded_password")
                .role(teacherRole)
                .enabled(true)
                .build());

        manager = userRepository.save(User.builder()
                .firstName("Manager")
                .lastName("Test")
                .email("manager@test.com")
                .password("encoded_password")
                .role(managerRole)
                .enabled(true)
                .build());
        userRepository.save(manager);

        student1 = studentRepository.save(Student.builder()
                .firstName("Student1")
                .lastName("Test")
                .email("student1@test.com")
                .phone("123456789")
                .build());

        student2 = studentRepository.save(Student.builder()
                .firstName("Student2")
                .lastName("Test")
                .email("student2@test.com")
                .phone("987654321")
                .build());

        group1 = StudyGroup.builder()
                .name("Morning Java")
                .course(testCourse)
                .teacher(teacher)
                .startDate(LocalDate.now())
                .status(GroupStatus.ACTIVE)
                .students(new HashSet<>(Set.of(student1, student2)))
                .build();

        group2 = StudyGroup.builder()
                .name("Evening Java")
                .course(testCourse)
                .teacher(teacher)
                .startDate(LocalDate.now().plusDays(7))
                .status(GroupStatus.ACTIVE)
                .students(new HashSet<>(Set.of(student2)))
                .build();

        group3 = StudyGroup.builder()
                .name("Advanced Java")
                .course(testCourse)
                .teacher(teacher)
                .startDate(LocalDate.now().plusDays(14))
                .status(GroupStatus.COMPLETED)
                .students(new HashSet<>())
                .build();
        studyGroupRepository.saveAll(List.of(group1, group2, group3));

        User studentUser = User.builder()
                .email("student@test.com")
                .password("encoded_password")
                .firstName("StudentUser")
                .lastName("Test")
                .role(studentRole)
                .enabled(true)
                .build();
        userRepository.save(studentUser);
        student1.setUser(studentUser);
        studentRepository.save(student1);

        teacher2 = userRepository.save(User.builder()
                .firstName("Teacher2")
                .lastName("Test")
                .email("teacher2@test.com")
                .password("encoded_password")
                .role(teacherRole)
                .enabled(true)
                .build());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createGroup_shouldReturn201AndDraftStatus() throws Exception {
        StudyGroupCreateRequest request = StudyGroupCreateRequest.builder()
                .name("Integration Group")
                .courseId(testCourse.getId())
                .teacherId(teacher.getId())
                .startDate(LocalDate.now().plusDays(7))
                .studentIds(Set.of(student1.getId(), student2.getId()))
                .build();

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/groups")
                        .contentType(APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Integration Group"))
                .andExpect(jsonPath("$.status").value(GroupStatus.DRAFT.name()))
                .andExpect(jsonPath("$.course.id").value(testCourse.getId()))
                .andExpect(jsonPath("$.teacher.id").value(teacher.getId()))
                .andExpect(jsonPath("$.students").isArray())
                .andExpect(jsonPath("$.students.length()").value(2))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createGroup_courseNotFound_shouldReturn404() throws Exception {
        StudyGroupCreateRequest request = StudyGroupCreateRequest.builder()
                .name("Invalid Course Group")
                .courseId(999L)
                .teacherId(teacher.getId())
                .startDate(LocalDate.now().plusDays(7))
                .build();

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/groups")
                        .contentType(APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("resource-not-found"))
                .andExpect(jsonPath("$.detail").value(containsString("Course")))
                .andExpect(jsonPath("$.detail").value(containsString("999")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createGroup_teacherIsManager_shouldReturn400() throws Exception {
        StudyGroupCreateRequest request = StudyGroupCreateRequest.builder()
                .name("Group with Manager as Teacher")
                .courseId(testCourse.getId())
                .teacherId(manager.getId())
                .startDate(LocalDate.now().plusDays(7))
                .build();

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/groups")
                        .contentType(APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("bad-request"))
                .andExpect(jsonPath("$.detail").value(containsString("не является учителем")))
                .andExpect(jsonPath("$.detail").value(containsString(manager.getId().toString())));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void createGroup_withTeacherRole_shouldReturn403() throws Exception {
        StudyGroupCreateRequest request = StudyGroupCreateRequest.builder()
                .name("Group by Teacher")
                .courseId(testCourse.getId())
                .teacherId(teacher.getId())
                .startDate(LocalDate.now().plusDays(7))
                .build();

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/groups")
                        .contentType(APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    @DisplayName("Список без фильтров — должен вернуть все группы (3)")
    void getAllStudyGroups_noFilters_shouldReturnAllGroups() throws Exception {
        mockMvc.perform(get("/api/v1/groups")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }


    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    @DisplayName("Фильтр по courseId — должен вернуть только группы курса Java Core")
    void getAllStudyGroups_filterByCourseId_shouldReturnOnlyMatchingGroups() throws Exception {
        mockMvc.perform(get("/api/v1/groups")
                        .param("courseId", testCourse.getId().toString())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[*].course.id").value(
                        org.hamcrest.Matchers.everyItem(
                                org.hamcrest.Matchers.is(
                                        Integer.valueOf(testCourse.getId().intValue())
                                )
                        )
                ));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    @DisplayName("Фильтр по status ACTIVE — должен вернуть только активные группы")
    void getAllStudyGroups_filterByStatusActive_shouldReturnOnlyActiveGroups() throws Exception {
        mockMvc.perform(get("/api/v1/groups")
                        .param("status", GroupStatus.ACTIVE.name())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].status").value(everyItem(is(GroupStatus.ACTIVE.name()))));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    @DisplayName("Пагинация: page=0&size=2 — должен вернуть 2 группы")
    void getAllStudyGroups_pagination_shouldReturnPageWith2Groups() throws Exception {
        mockMvc.perform(get("/api/v1/groups")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "id,asc")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @WithUserDetails(
            value = "teacher@test.com",
            setupBefore = TestExecutionEvent.TEST_EXECUTION
    )
    @DisplayName("TEACHER может видеть список своих групп — 200")
    void getAllStudyGroups_withTeacherRole_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/groups")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /{id}/status: DRAFT → ACTIVE → 200")
    void updateStatus_DraftToActive_ShouldReturn200() throws Exception {

        StudyGroup draftGroup = StudyGroup.builder()
                .name("Draft Group")
                .course(testCourse)
                .teacher(teacher)
                .startDate(LocalDate.now())
                .status(GroupStatus.DRAFT)
                .students(new HashSet<>())
                .build();
        draftGroup = studyGroupRepository.save(draftGroup);

        GroupStatusUpdateRequest request = new GroupStatusUpdateRequest(GroupStatus.ACTIVE);
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(patch("/api/v1/groups/{id}/status", draftGroup.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(GroupStatus.ACTIVE.name()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /{id}/status: DRAFT → COMPLETED → 400")
    void updateStatus_DraftToCompleted_ShouldReturn400() throws Exception {
        StudyGroup draftGroup = StudyGroup.builder()
                .name("Draft Group 2")
                .course(testCourse)
                .teacher(teacher)
                .startDate(LocalDate.now())
                .status(GroupStatus.DRAFT)
                .students(new HashSet<>())
                .build();
        draftGroup = studyGroupRepository.save(draftGroup);

        GroupStatusUpdateRequest request = new GroupStatusUpdateRequest(GroupStatus.COMPLETED);
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(patch("/api/v1/groups/{id}/status", draftGroup.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("bad-request"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /{id}/status: COMPLETED → ACTIVE → 400")
    void updateStatus_CompletedToActive_ShouldReturn400() throws Exception {

        StudyGroup completedGroup = StudyGroup.builder()
                .name("Completed Group")
                .course(testCourse)
                .teacher(teacher)
                .startDate(LocalDate.now().minusDays(30))
                .status(GroupStatus.COMPLETED)
                .students(new HashSet<>())
                .build();
        completedGroup = studyGroupRepository.save(completedGroup);

        GroupStatusUpdateRequest request = new GroupStatusUpdateRequest(GroupStatus.ACTIVE);
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(patch("/api/v1/groups/{id}/status", completedGroup.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("bad-request"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("PATCH /{id}/status: TEACHER → 403")
    void updateStatus_WithTeacherRole_ShouldReturn403() throws Exception {

        StudyGroup group = StudyGroup.builder()
                .name("Test Group")
                .course(testCourse)
                .teacher(teacher)
                .startDate(LocalDate.now())
                .status(GroupStatus.DRAFT)
                .students(new HashSet<>())
                .build();
        group = studyGroupRepository.save(group);

        GroupStatusUpdateRequest request = new GroupStatusUpdateRequest(GroupStatus.ACTIVE);
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(patch("/api/v1/groups/{id}/status", group.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /{id}/status: группа не найдена → 404")
    void updateStatus_GroupNotFound_ShouldReturn404() throws Exception {
        // given
        GroupStatusUpdateRequest request = new GroupStatusUpdateRequest(GroupStatus.ACTIVE);
        String requestJson = objectMapper.writeValueAsString(request);
        Long nonExistentId = 999L;

        // when & then
        mockMvc.perform(patch("/api/v1/groups/{id}/status", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("resource-not-found"));
    }


    @Test
    @DisplayName("Успешное обновление учебной группы со статусом DRAFT")
    void updateStudyGroup_WhenValidRequestAndDraftStatus_Returns200AndUpdatedGroup() throws Exception {

        StudyGroup draftGroup = StudyGroup.builder()
                .name("Old Draft Name")
                .course(testCourse)
                .teacher(teacher)
                .startDate(LocalDate.now())
                .status(GroupStatus.DRAFT)
                .students(new java.util.HashSet<>(java.util.Set.of(student1)))
                .build();
        StudyGroup savedGroup = studyGroupRepository.save(draftGroup);


        StudyGroupUpdateRequest updateRequest = StudyGroupUpdateRequest.builder()
                .name("Updated Group Name")
                .courseId(testCourse.getId())
                .teacherId(teacher.getId())
                .startDate(LocalDate.now().plusDays(5))
                .build();


        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/groups/{id}", savedGroup.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(manager.getEmail()).roles("MANAGER"))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.id").value(savedGroup.getId()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.name").value("Updated Group Name"));


        StudyGroup updatedInDb = studyGroupRepository.findById(savedGroup.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("Updated Group Name", updatedInDb.getName());
        org.junit.jupiter.api.Assertions.assertEquals(LocalDate.now().plusDays(5), updatedInDb.getStartDate());
    }

    @Test
    @DisplayName("Тест 2: Попытка обновить группу в финальном статусе COMPLETED должна возвращать 400")
    void updateStudyGroup_WhenStatusIsCompleted_Returns400BadRequest() throws Exception {

        StudyGroup completedGroup = StudyGroup.builder()
                .name("Completed Java Group")
                .course(testCourse)
                .teacher(teacher)
                .startDate(LocalDate.now().minusDays(30))
                .status(GroupStatus.COMPLETED)
                .students(new java.util.HashSet<>())
                .build();
        StudyGroup savedGroup = studyGroupRepository.save(completedGroup);

        StudyGroupUpdateRequest updateRequest = StudyGroupUpdateRequest.builder()
                .name("Attempt to Rename Completed")
                .courseId(testCourse.getId())
                .teacherId(teacher.getId())
                .startDate(LocalDate.now())
                .build();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/groups/{id}", savedGroup.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(manager.getEmail()).roles("MANAGER"))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.type").value("bad-request"));
    }

    @Test
    @DisplayName("Тест 3: Обновление группы с назначением пользователя без роли TEACHER должно возвращать 400")
    void updateStudyGroup_WhenNewTeacherHasWrongRole_Returns400BadRequest() throws Exception {

        StudyGroup draftGroup = StudyGroup.builder()
                .name("Valid Draft Group")
                .course(testCourse)
                .teacher(teacher)
                .startDate(LocalDate.now())
                .status(GroupStatus.DRAFT)
                .students(new java.util.HashSet<>())
                .build();
        StudyGroup savedGroup = studyGroupRepository.save(draftGroup);

        StudyGroupUpdateRequest updateRequest = StudyGroupUpdateRequest.builder()
                .name("Updated Group Name")
                .courseId(testCourse.getId())
                .teacherId(manager.getId())
                .startDate(LocalDate.now().plusDays(5))
                .build();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/groups/{id}", savedGroup.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(manager.getEmail()).roles("MANAGER"))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @DisplayName("Тест 4: Пользователь с ролью TEACHER не имеет прав на обновление группы и получает 403")
    void updateStudyGroup_WhenUserIsTeacher_Returns403Forbidden() throws Exception {

        StudyGroup draftGroup = StudyGroup.builder()
                .name("Valid Draft Group")
                .course(testCourse)
                .teacher(teacher)
                .startDate(LocalDate.now())
                .status(GroupStatus.DRAFT)
                .students(new java.util.HashSet<>())
                .build();
        StudyGroup savedGroup = studyGroupRepository.save(draftGroup);

        StudyGroupUpdateRequest updateRequest = StudyGroupUpdateRequest.builder()
                .name("Updated Group Name")
                .courseId(testCourse.getId())
                .teacherId(teacher.getId())
                .startDate(LocalDate.now().plusDays(5))
                .build();


        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/groups/{id}", savedGroup.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(teacher.getEmail()).roles("TEACHER"))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))

                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
    }

    @WithUserDetails(value = "admin@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Тест 1: ADMIN получает любую группу → 200")
    void getGroupById_Admin_ShouldReturn200() throws Exception {
        Long groupId = studyGroupRepository.findAll().get(0).getId();
        mockMvc.perform(get("/api/v1/groups/{id}", groupId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(groupId));
    }

    @WithUserDetails(value = "teacher@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Тест 2: TEACHER получает свою группу → 200")
    void getGroupById_TeacherOwnGroup_ShouldReturn200() throws Exception {
        StudyGroup group = studyGroupRepository.findAll().stream()
                .filter(g -> g.getTeacher().getId().equals(teacher.getId()))
                .findFirst()
                .orElseThrow();
        mockMvc.perform(get("/api/v1/groups/{id}", group.getId())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(group.getId()));
    }

    @WithUserDetails(value = "teacher2@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Тест 3: TEACHER пытается получить чужую группу → 403")
    void getGroupById_TeacherOtherGroup_ShouldReturn403() throws Exception {
        StudyGroup group = studyGroupRepository.findAll().stream()
                .filter(g -> !g.getTeacher().getId().equals(teacher2.getId()))
                .findFirst()
                .orElseThrow();
        mockMvc.perform(get("/api/v1/groups/{id}", group.getId())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @WithUserDetails(value = "student@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Тест 4: STUDENT получает свою группу (состоит в группе) → 200")
    void getGroupById_StudentOwnGroup_ShouldReturn200() throws Exception {
        StudyGroup group = studyGroupRepository.findAll().stream()
                .filter(g -> g.getStudents().stream().anyMatch(s -> s.getId().equals(student1.getId())))
                .findFirst()
                .orElseThrow();
        mockMvc.perform(get("/api/v1/groups/{id}", group.getId())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(group.getId()));
    }

    @WithUserDetails(value = "student@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Тест 5: STUDENT пытается получить чужую группу (не состоит) → 403")
    void getGroupById_StudentOtherGroup_ShouldReturn403() throws Exception {
        StudyGroup group = studyGroupRepository.findAll().stream()
                .filter(g -> g.getStudents().isEmpty())
                .findFirst()
                .orElseThrow();
        mockMvc.perform(get("/api/v1/groups/{id}", group.getId())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @WithUserDetails(value = "admin@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("Тест 6: группа не найдена → 404")
    void getGroupById_GroupNotFound_ShouldReturn404() throws Exception {
        Long nonExistentId = 99999L;
        mockMvc.perform(get("/api/v1/groups/{id}", nonExistentId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("resource-not-found"));
    }


    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("DELETE /{id}/students/{studentId} — успешное удаление -> 204, студент удалён из БД")
    void removeStudent_Success_ShouldReturn204() throws Exception {
        Student student = studentRepository.save(Student.builder()
                .firstName("TestStudent")
                .lastName("Remove")
                .email("remove@test.com")
                .build());

        StudyGroup group = StudyGroup.builder()
                .name("Remove Test Group")
                .course(testCourse)
                .teacher(teacher)
                .startDate(LocalDate.now())
                .status(GroupStatus.DRAFT)
                .students(new HashSet<>(Set.of(student)))
                .build();
        group = studyGroupRepository.save(group);

        mockMvc.perform(delete("/api/v1/groups/{id}/students/{studentId}", group.getId(), student.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$").doesNotExist());

        Long groupId = group.getId();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            StudyGroup updatedGroup = studyGroupRepository.findById(groupId).orElseThrow();
            assertThat(updatedGroup.getStudents()).doesNotContain(student);
        });
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("DELETE /{id}/students/{studentId} — студент не состоит в группе -> 400 bad-request")
    void removeStudent_StudentNotInGroup_ShouldReturn400() throws Exception {
        Student student = studentRepository.save(Student.builder()
                .firstName("StudentNotInGroup")
                .lastName("Test")
                .email("notin@test.com")
                .build());

        StudyGroup group = StudyGroup.builder()
                .name("Empty Group")
                .course(testCourse)
                .teacher(teacher)
                .startDate(LocalDate.now())
                .status(GroupStatus.DRAFT)
                .students(new HashSet<>())
                .build();
        group = studyGroupRepository.save(group);

        mockMvc.perform(delete("/api/v1/groups/{id}/students/{studentId}", group.getId(), student.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("bad-request"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("DELETE /{id}/students/{studentId} — группа COMPLETED -> 400")
    void removeStudent_GroupCompleted_ShouldReturn400() throws Exception {
        Student student = studentRepository.save(Student.builder()
                .firstName("CompletedStudent")
                .lastName("Test")
                .email("completed@test.com")
                .build());

        StudyGroup group = StudyGroup.builder()
                .name("Completed Group")
                .course(testCourse)
                .teacher(teacher)
                .startDate(LocalDate.now().minusDays(30))
                .status(GroupStatus.COMPLETED)
                .students(new HashSet<>(Set.of(student)))
                .build();
        group = studyGroupRepository.save(group);

        mockMvc.perform(delete("/api/v1/groups/{id}/students/{studentId}", group.getId(), student.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("bad-request"));

        Long groupId = group.getId();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            StudyGroup unchangedGroup = studyGroupRepository.findById(groupId).orElseThrow();
            assertThat(unchangedGroup.getStudents()).contains(student);
        });
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("DELETE /{id}/students/{studentId} — TEACHER не может удалить студента из группы -> 403")
    void removeStudent_TeacherRole_ShouldReturn403() throws Exception {
        Student student = studentRepository.save(Student.builder()
                .firstName("TeacherStudent")
                .lastName("Test")
                .email("teacherrole@test.com")
                .build());

        StudyGroup group = StudyGroup.builder()
                .name("Teacher Role Group")
                .course(testCourse)
                .teacher(teacher)
                .startDate(LocalDate.now())
                .status(GroupStatus.DRAFT)
                .students(new HashSet<>(Set.of(student)))
                .build();
        group = studyGroupRepository.save(group);

        mockMvc.perform(delete("/api/v1/groups/{id}/students/{studentId}", group.getId(), student.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("DELETE /{id}/students/{studentId} — группа не найдена -> 404 resource-not-found")
    void removeStudent_GroupNotFound_ShouldReturn404() throws Exception {
        Student student = studentRepository.save(Student.builder()
                .firstName("NotFoundStudent")
                .lastName("Test")
                .email("notfound@test.com")
                .build());

        Long nonExistentGroupId = 9999L;

        mockMvc.perform(delete("/api/v1/groups/{id}/students/{studentId}", nonExistentGroupId, student.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("resource-not-found"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("Успешное добавление студента в группу → 200, студент присутствует в списке")
    void addStudent_Success_shouldReturn200() throws Exception {
        Long groupId = group2.getId();
        AddStudentToGroupRequest request = new AddStudentToGroupRequest(student1.getId());
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/groups/{id}/students", groupId)
                        .contentType(APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(groupId))
                .andExpect(jsonPath("$.students[*].id").value(org.hamcrest.Matchers.hasItem(student1.getId().intValue())));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("Студент уже в группе → 409 Conflict")
    void addStudent_StudentAlreadyInGroup_shouldReturn409() throws Exception {
        Long groupId = group1.getId();
        AddStudentToGroupRequest request = new AddStudentToGroupRequest(student1.getId());
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/groups/{id}/students", groupId)
                        .contentType(APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("conflict"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("Группа в финальном статусе COMPLETED → 400 BadRequest")
    void addStudent_GroupCompleted_shouldReturn400() throws Exception {
        Long groupId = group3.getId();
        AddStudentToGroupRequest request = new AddStudentToGroupRequest(student1.getId());
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/groups/{id}/students", groupId)
                        .contentType(APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("bad-request"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("TEACHER не может добавить студента → 403 Forbidden")
    void addStudent_TeacherRole_shouldReturn403() throws Exception {
        Long groupId = group2.getId();
        AddStudentToGroupRequest request = new AddStudentToGroupRequest(student1.getId());
        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/groups/{id}/students", groupId)
                        .contentType(APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "admin@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("GET /{id}/students — ADMIN получает студентов группы -> 200")
    void getStudentsByGroupId_Admin_ShouldReturn200() throws Exception {
        Long groupId = studyGroupRepository.findAll().stream()
                .findFirst()
                .map(StudyGroup::getId)
                .orElseThrow();

        mockMvc.perform(get("/api/v1/groups/{id}/students", groupId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].firstName").exists());
    }

    @Test
    @WithUserDetails(value = "teacher@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("GET /{id}/students — TEACHER получает студентов своей группы -> 200")
    void getStudentsByGroupId_TeacherOwnGroup_ShouldReturn200() throws Exception {
        Long groupId = studyGroupRepository.findAll().stream()
                .filter(g -> g.getTeacher().getId().equals(teacher.getId()))
                .findFirst()
                .map(StudyGroup::getId)
                .orElseThrow();

        mockMvc.perform(get("/api/v1/groups/{id}/students", groupId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithUserDetails(value = "teacher2@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("GET /{id}/students — TEACHER пытается получить студентов чужой группы -> 403")
    void getStudentsByGroupId_TeacherOtherGroup_ShouldReturn403() throws Exception {
        Long groupId = studyGroupRepository.findAll().stream()
                .filter(g -> !g.getTeacher().getId().equals(teacher2.getId()))
                .findFirst()
                .map(StudyGroup::getId)
                .orElseThrow();

        mockMvc.perform(get("/api/v1/groups/{id}/students", groupId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "student@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("GET /{id}/students — STUDENT не имеет доступа к списку студентов группы -> 403")
    void getStudentsByGroupId_StudentRole_ShouldReturn403() throws Exception {
        Long groupId = studyGroupRepository.findAll().stream()
                .findFirst()
                .map(StudyGroup::getId)
                .orElseThrow();

        mockMvc.perform(get("/api/v1/groups/{id}/students", groupId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = "admin@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("GET /{id}/students — группа не найдена -> 404")
    void getStudentsByGroupId_GroupNotFound_ShouldReturn404() throws Exception {
        Long nonExistentId = 99999L;

        mockMvc.perform(get("/api/v1/groups/{id}/students", nonExistentId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("resource-not-found"));
    }

    @Test
    @WithUserDetails(value = "admin@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @DisplayName("GET /{id}/students — пустая группа -> 200 с пустым массивом")
    void getStudentsByGroupId_EmptyGroup_ShouldReturn200WithEmptyArray() throws Exception {
        StudyGroup emptyGroup = studyGroupRepository.save(StudyGroup.builder()
                .name("Empty Group for Test")
                .course(testCourse)
                .teacher(teacher)
                .startDate(LocalDate.now().plusDays(7))
                .status(GroupStatus.DRAFT)
                .students(new HashSet<>())
                .build());

        mockMvc.perform(get("/api/v1/groups/{id}/students", emptyGroup.getId())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
