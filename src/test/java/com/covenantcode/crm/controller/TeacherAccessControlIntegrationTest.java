package com.covenantcode.crm.controller;

import com.covenantcode.crm.BaseIntegrationTest;
import com.covenantcode.crm.entity.Course;
import com.covenantcode.crm.entity.Lesson;
import com.covenantcode.crm.entity.Role;
import com.covenantcode.crm.entity.Student;
import com.covenantcode.crm.entity.StudyGroup;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.CourseStatus;
import com.covenantcode.crm.entity.enums.GroupStatus;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.repository.CourseRepository;
import com.covenantcode.crm.repository.LessonRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeacherAccessControlIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StudyGroupRepository studyGroupRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User teacher1;
    private User teacher2;

    private StudyGroup group1;
    private StudyGroup group2;

    private Student student1;
    private Student student2;

    private Lesson lesson1;
    private Lesson lesson2;

    private String teacher1Token;

    @BeforeEach
    void setUp() throws Exception {

        Role teacherRole = roleRepository.findByName(RoleName.TEACHER)
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name(RoleName.TEACHER)
                                .build()
                ));

        teacher1 = userRepository.save(User.builder()
                .firstName("Teacher")
                .lastName("One")
                .email("teacher1@mail.com")
                .password(passwordEncoder.encode("password"))
                .role(teacherRole)
                .enabled(true)
                .build());

        teacher2 = userRepository.save(User.builder()
                .firstName("Teacher")
                .lastName("Two")
                .email("teacher2@mail.com")
                .password(passwordEncoder.encode("password"))
                .role(teacherRole)
                .enabled(true)
                .build());

        Course course1 = courseRepository.save(Course.builder()
                .title("Java Core")
                .price(BigDecimal.valueOf(100))
                .durationInWeeks(8)
                .status(CourseStatus.ACTIVE)
                .build());

        Course course2 = courseRepository.save(Course.builder()
                .title("Spring Boot")
                .price(BigDecimal.valueOf(150))
                .durationInWeeks(12)
                .status(CourseStatus.ACTIVE)
                .build());

        student1 = studentRepository.save(Student.builder()
                .firstName("Student")
                .lastName("One")
                .email("student1@mail.com")
                .build());

        student2 = studentRepository.save(Student.builder()
                .firstName("Student")
                .lastName("Two")
                .email("student2@mail.com")
                .build());

        group1 = studyGroupRepository.save(StudyGroup.builder()
                .name("GROUP-1")
                .course(course1)
                .teacher(teacher1)
                .startDate(LocalDate.now().plusDays(7))
                .status(GroupStatus.ACTIVE)
                .students(new HashSet<>(Set.of(student1)))
                .build());

        group2 = studyGroupRepository.save(StudyGroup.builder()
                .name("GROUP-2")
                .course(course2)
                .teacher(teacher2)
                .startDate(LocalDate.now().plusDays(7))
                .status(GroupStatus.ACTIVE)
                .students(new HashSet<>(Set.of(student2)))
                .build());

        lesson1 = lessonRepository.save(Lesson.builder()
                .studyGroup(group1)
                .teacher(teacher1)
                .topic("Lesson 1")
                .lessonDate(LocalDate.now().plusDays(10))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(20, 0))
                .build());

        lesson2 = lessonRepository.save(Lesson.builder()
                .studyGroup(group2)
                .teacher(teacher2)
                .topic("Lesson 2")
                .lessonDate(LocalDate.now().plusDays(11))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(20, 0))
                .build());

        teacher1Token = loginAndGetToken("teacher1@mail.com", "password");
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    @Test
    @DisplayName("Тест 1: TEACHER видит только свои группы")
    void teacherSeesOnlyOwnGroups() throws Exception {
        mockMvc.perform(get("/api/v1/groups")
                        .header("Authorization", "Bearer " + teacher1Token)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(group1.getId()))
                .andExpect(jsonPath("$.content[0].name").value("GROUP-1"));
    }

    @Test
    @DisplayName("Тест 2: TEACHER не видит чужую группу")
    void teacherCannotSeeForeignGroup() throws Exception {
        mockMvc.perform(get("/api/v1/groups/" + group2.getId())
                        .header("Authorization", "Bearer " + teacher1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Тест 3: TEACHER видит только своих студентов")
    void teacherSeesOnlyOwnStudents() throws Exception {
        mockMvc.perform(get("/api/v1/students")
                        .header("Authorization", "Bearer " + teacher1Token)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(student1.getId()))
                .andExpect(jsonPath("$.content[0].firstName").value("Student"))
                .andExpect(jsonPath("$.content[0].lastName").value("One"))
                .andExpect(jsonPath("$.content[0].email").value("student1@mail.com"));
    }

    @Test
    @DisplayName("Тест 4: TEACHER не видит студента чужой группы")
    void teacherCannotSeeForeignStudent() throws Exception {
        mockMvc.perform(get("/api/v1/students/" + student2.getId())
                        .header("Authorization", "Bearer " + teacher1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Тест 5: TEACHER видит только свои занятия")
    void teacherSeesOnlyOwnLessons() throws Exception {
        mockMvc.perform(get("/api/v1/lessons")
                        .header("Authorization", "Bearer " + teacher1Token)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(lesson1.getId()))
                .andExpect(jsonPath("$.content[0].topic").value("Lesson 1"));
    }

    @Test
    @DisplayName("Тест 6: TEACHER не может создать курс")
    void teacherCannotCreateCourse() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                        .header("Authorization", "Bearer " + teacher1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "New Course",
                                  "description": "Test description",
                                  "durationInWeeks": 8,
                                  "price": 199.99
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Тест 7: TEACHER не может просматривать лиды")
    void teacherCannotViewLeads() throws Exception {
        mockMvc.perform(get("/api/v1/leads")
                        .header("Authorization", "Bearer " + teacher1Token)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Тест 8: TEACHER видит расписание только своих групп")
    void teacherCannotSeeForeignGroupLessons() throws Exception {
        mockMvc.perform(get("/api/v1/groups/" + group2.getId() + "/lessons")
                        .header("Authorization", "Bearer " + teacher1Token))
                .andExpect(status().isForbidden());
    }
}
