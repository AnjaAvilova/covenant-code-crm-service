package com.covenantcode.crm.controller;

import com.covenantcode.crm.BaseIntegrationTest;
import com.covenantcode.crm.entity.Course;
import com.covenantcode.crm.entity.Lead;
import com.covenantcode.crm.entity.Role;
import com.covenantcode.crm.entity.Student;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.CourseStatus;
import com.covenantcode.crm.entity.enums.LeadStatus;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.repository.CourseRepository;
import com.covenantcode.crm.repository.LeadRepository;
import com.covenantcode.crm.repository.RoleRepository;
import com.covenantcode.crm.repository.StudentRepository;
import com.covenantcode.crm.repository.UserRepository;
import com.covenantcode.crm.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ExportControllerIntegrationTest extends BaseIntegrationTest {

    private static final String EXPORT_LEADS_URL = "/api/v1/export/leads";
    private static final String EXPORT_STUDENTS_URL = "/api/v1/export/students";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private JwtService jwtService;

    private String adminToken;

    @BeforeEach
    void setUp() {

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ADMIN).build()));

        User adminUser = User.builder()
                .firstName("Admin")
                .lastName("Test")
                .email("admin@test.com")
                .password(passwordEncoder.encode("admin123"))
                .role(adminRole)
                .enabled(true)
                .build();
        adminUser = userRepository.save(adminUser);
        adminToken = jwtService.generateToken(adminUser);

        Course testCourse = Course.builder()
                .title("Java Backend Developer")
                .description("Full course")
                .durationInWeeks(12)
                .price(BigDecimal.valueOf(45000.00))
                .status(CourseStatus.ACTIVE)
                .build();
        testCourse = courseRepository.save(testCourse);

        Lead testLead = Lead.builder()
                .firstName("Иван")
                .lastName("Петров")
                .phone("+79001234567")
                .email("ivan@example.com")
                .status(LeadStatus.IN_PROGRESS)
                .interestedCourse(testCourse)
                .source("website")
                .build();
        leadRepository.save(testLead);

        Student student1 = Student.builder()
                .firstName("Иван")
                .lastName("Смирнов")
                .phone("+79161112233")
                .email("ivan.smirnov@example.com")
                .build();
        studentRepository.save(student1);

        Student student2 = Student.builder()
                .firstName("Петр")
                .lastName("Иванов")
                .phone("+79162223344")
                .email("petr.ivanov@example.com")
                .build();
        studentRepository.save(student2);
    }

    @Test
    @DisplayName("экспорт лидов (200, Content-Type csv)")
    void exportLeads_shouldReturn200WithCsvContent() throws Exception {
        mockMvc.perform(get(EXPORT_LEADS_URL)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(content().string(containsString("Иван")))
                .andExpect(content().string(containsString("Петров")))
                .andExpect(content().string(containsString("+79001234567")))
                .andExpect(content().string(containsString("ivan@example.com")))
                .andExpect(content().string(containsString("IN_PROGRESS")))
                .andExpect(content().string(containsString("Java Backend Developer")));
    }

    @Test
    @DisplayName("Тест 2: экспорт студентов с search-фильтром")
    void exportStudents_withSearchFilter_shouldReturnOnlyMatchingStudents() throws Exception {
        mockMvc.perform(get(EXPORT_STUDENTS_URL)
                        .param("search", "иван")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                // Проверяем: в CSV есть студент Иван Смирнов
                .andExpect(content().string(containsString("Иван")))
                .andExpect(content().string(containsString("Смирнов")))
                .andExpect(content().string(containsString("ivan.smirnov@example.com")))
                .andExpect(content().string(containsString("+79161112233")))
                // Проверяем: в CSV нет студента Анна Петрова
                .andExpect(content().string(not(containsString("Анна"))))
                .andExpect(content().string(not(containsString("Петрова"))))
                .andExpect(content().string(not(containsString("anna.petrova@example.com"))));
    }

    @Test
    @DisplayName("Тест 3: доступ без токена (401)")
    void exportLeads_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get(EXPORT_LEADS_URL))
                .andExpect(status().isUnauthorized());
    }
}
