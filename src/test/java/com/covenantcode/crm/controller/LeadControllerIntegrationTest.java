package com.covenantcode.crm.controller;

import com.covenantcode.crm.BaseIntegrationTest;
import com.covenantcode.crm.dto.lead.LeadCommentCreateRequest;
import com.covenantcode.crm.dto.lead.LeadCommentResponse;
import com.covenantcode.crm.dto.lead.LeadConvertRequest;
import com.covenantcode.crm.dto.lead.LeadCreateRequest;
import com.covenantcode.crm.dto.lead.LeadStatusUpdateRequest;
import com.covenantcode.crm.dto.lead.LeadUpdateRequest;
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
import com.covenantcode.crm.repository.UserRepository;
import com.covenantcode.crm.security.JwtService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "MANAGER")
public class LeadControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private JwtService jwtService;

    private Course testCourse;
    private User testManager;
    private User testAdmin;
    private User testStudent;
    private String managerToken;
    private String adminToken;
    protected String studentToken;
    private final String baseUrl = "/api/v1/leads";
    private static final String COMMENTS_ENDPOINT = "/api/v1/leads/{id}/comments";

    @BeforeEach
    void setUp() {
        leadRepository.deleteAll();
        userRepository.deleteAll();
        courseRepository.deleteAll();
        roleRepository.deleteAll();

        testCourse = new Course();
        testCourse.setTitle("Test Course");
        testCourse.setDescription("Integration test course");
        testCourse.setPrice(BigDecimal.valueOf(199.99));
        testCourse.setDurationInWeeks(8);
        testCourse.setStatus(CourseStatus.ACTIVE);
        testCourse = courseRepository.save(testCourse);

        Role managerRole = roleRepository.findByName(RoleName.MANAGER)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(RoleName.MANAGER);
                    return roleRepository.save(newRole);
                });

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
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

        testManager = new User();
        testManager.setFirstName("Manager");
        testManager.setLastName("Test");
        testManager.setEmail("manager@test.com");
        testManager.setPassword(passwordEncoder.encode("password"));
        testManager.setRole(managerRole);
        testManager.setEnabled(true);
        testManager = userRepository.save(testManager);

        testAdmin = new User();
        testAdmin.setFirstName("Admin");
        testAdmin.setLastName("Test");
        testAdmin.setEmail("admin@test.com");
        testAdmin.setPassword(passwordEncoder.encode("password"));
        testAdmin.setRole(adminRole);
        testAdmin.setEnabled(true);
        testAdmin = userRepository.save(testAdmin);

        testStudent = new User();
        testStudent.setFirstName("Student");
        testStudent.setLastName("Test");
        testStudent.setEmail("student@test.com");
        testStudent.setPassword(passwordEncoder.encode("password"));
        testStudent.setRole(studentRole);
        testStudent.setEnabled(true);
        testStudent = userRepository.save(testStudent);

        managerToken = jwtService.generateToken(testManager);
        adminToken = jwtService.generateToken(testAdmin);
        studentToken = jwtService.generateToken(testStudent);
    }
    @AfterEach
    void tearDown() {
        // Удаляем все связанные сущности (в обратном порядке создания)
        leadRepository.deleteAll(); // добавлено для очистки лидов
        userRepository.deleteAll();   // удаление пользователей
        courseRepository.deleteAll(); // удаление курсов
        roleRepository.deleteAll();   // удаление ролей
    }


    // Вспомогательный метод для получения EntityManager (если нужно)
    private EntityManager getEntityManager() {
        return entityManager;
    }

    @Test
    @DisplayName("Создание лида со всеми полями -> HTTP 201, статус NEW, курс и менеджер проставлены")
    void createLead_fullFields_shouldReturn201() throws Exception {
        LeadCreateRequest request = new LeadCreateRequest();
        request.setFirstName("Ivan");
        request.setLastName("Petrov");
        request.setPhone("+79001234567");
        request.setEmail("ivan@example.com");
        request.setSource("website");
        request.setInterestedCourseId(testCourse.getId());
        request.setAssignedManagerId(testManager.getId());
        request.setComment("Integration test");

        mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value(LeadStatus.NEW.name()))
                .andExpect(jsonPath("$.interestedCourse.id").value(testCourse.getId()))
                .andExpect(jsonPath("$.interestedCourse.title").value(testCourse.getTitle()))
                .andExpect(jsonPath("$.assignedManager.id").value(testManager.getId()))
                .andExpect(jsonPath("$.assignedManager.firstName").value(testManager.getFirstName()))
                .andExpect(jsonPath("$.assignedManager.lastName").value(testManager.getLastName()));
    }

    @Test
    @DisplayName("Создание лида только с обязательными полями -> HTTP 201, статус NEW, курс и менеджер null")
    void createLead_minimalFields_shouldReturn201() throws Exception {
        LeadCreateRequest request = new LeadCreateRequest();
        request.setFirstName("John");
        request.setPhone("+79998887766");

        mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value(LeadStatus.NEW.name()))
                .andExpect(jsonPath("$.interestedCourse").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.assignedManager").value(Matchers.nullValue()));
    }

    @Test
    @DisplayName("Передан несуществующий interestedCourseId -> HTTP 404, тип ошибки resource-not-found")
    void createLead_invalidCourseId_shouldReturn404() throws Exception {
        LeadCreateRequest request = new LeadCreateRequest();
        request.setFirstName("Anna");
        request.setPhone("+79001112233");
        request.setInterestedCourseId(9999L);
        request.setAssignedManagerId(testManager.getId());

        mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("resource-not-found"))
                .andExpect(jsonPath("$.detail").value(containsString("Course")))
                .andExpect(jsonPath("$.detail").value(containsString("9999")));
    }

    @Test
    @DisplayName("Отсутствует firstName -> HTTP 400, ошибка валидации для поля firstName")
    void createLead_emptyFirstName_shouldReturn400() throws Exception {
        LeadCreateRequest request = new LeadCreateRequest();
        request.setPhone("+79001112233");
        mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("validation-error"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("firstName"))
                .andExpect(jsonPath("$.errors[0].message").value(containsString("обязательно")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getLead_whenLeadExists_thenHttp200WithAllFields() throws Exception {

        Lead lead = new Lead();
        lead.setFirstName("Ivan");
        lead.setLastName("Petrov");
        lead.setEmail("ivan@example.com");
        lead.setPhone("+79990000000");
        lead.setStatus(LeadStatus.NEW);

        Lead saved = leadRepository.saveAndFlush(lead);

        mockMvc.perform(get("/api/v1/leads/{id}", saved.getId())
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(saved.getId().intValue())))
                .andExpect(jsonPath("$.status", is(saved.getStatus().name())));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getLead_whenLeadNotExists_thenHttp404() throws Exception {

        mockMvc.perform(get("/api/v1/leads/{id}", 9999L)
                        .accept(APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type", is("resource-not-found")));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void getLead_whenUserHasTeacherRole_thenHttp403() throws Exception {
        Lead lead = new Lead();
        lead.setFirstName("Ivan");
        lead.setPhone("+79990000000");
        lead.setStatus(LeadStatus.NEW);
        Lead saved = leadRepository.saveAndFlush(lead);

        mockMvc.perform(get("/api/v1/leads/{id}", saved.getId())
                        .accept(APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Получить все лиды без фильтров - возвращает все лиды постранично")
    void testControllerIntegrationGetAllLeadsNoFiltersShouldReturnAllWithPagination() throws Exception {

        Lead lead1 = createLeadWithStatus(LeadStatus.NEW);
        Lead lead2 = createLeadWithStatus(LeadStatus.IN_PROGRESS);
        Lead lead3 = createLeadWithStatus(LeadStatus.CONTACTED);

        List<Long> leadIds = List.of(lead1.getId(), lead2.getId(), lead3.getId());

        mockMvc.perform(get(baseUrl)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.content", Matchers.hasSize(3)))
                .andExpect(jsonPath("$.content[*].id", Matchers.containsInAnyOrder(
                        lead1.getId().intValue(),
                        lead2.getId().intValue(),
                        lead3.getId().intValue()
                )));
    }

    @Test
    @DisplayName("Фильтрация лидов по статусу NEW - возвращает только NEW лиды")
    void testControllerIntegrationGetAllLeadsByStatusNewShouldReturnOnlyNew() throws Exception {

        Lead lead1 = createLeadWithStatus(LeadStatus.NEW);
        Lead lead2 = createLeadWithStatus(LeadStatus.NEW);
        Lead lead3 = createLeadWithStatus(LeadStatus.IN_PROGRESS);

        mockMvc.perform(get(baseUrl)
                        .param("status", LeadStatus.NEW.name())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.content[*].status", Matchers.everyItem(is(LeadStatus.NEW.name()))));
    }

    @Test
    @DisplayName("Поиск лидов по части телефона - возвращает лиды с совпадающим номером")
    void testControllerIntegrationSearchLeadsByPhonePartShouldReturnMatchingLeads() throws Exception {

        Lead lead = new Lead();
        lead.setFirstName("Client");
        lead.setPhone("+79161234567");
        lead.setStatus(LeadStatus.NEW);
        leadRepository.saveAndFlush(lead);

        mockMvc.perform(get(baseUrl)
                        .param("search", "9161")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].phone", is("+79161234567")));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("Попытка доступа с ролью TEACHER к эндпоинту getAll -> HTTP 403")
    void testControllerIntegrationGetAllLeadsWithTeacherRoleShouldReturn403() throws Exception {

        for (int i = 0; i < 3; i++) {
            createLeadWithStatus(LeadStatus.NEW);
        }

        mockMvc.perform(get(baseUrl)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isForbidden());
    }

    private Lead createLeadWithStatus(LeadStatus status) {
        Lead lead = new Lead();
        lead.setFirstName("Test_" + status.name());
        lead.setPhone("+7900" + (int) (Math.random() * 100000000));
        lead.setStatus(status);
        return leadRepository.saveAndFlush(lead);
    }

    @Test
    @DisplayName("POST /{id}/convert — Успех (201) от MANAGER")
    @WithMockUser(roles = "MANAGER")
    void convertLead_Success_Manager() throws Exception {

        Lead lead = createLeadWithStatus(LeadStatus.NEW);

        LeadConvertRequest request = LeadConvertRequest.builder()
                .firstName("Студент")
                .lastName("Тестовый")
                .phone("+79998887766")
                .email("student@test.com")
                .birthDate(LocalDate.of(2000, 1, 1))
                .build();

        mockMvc.perform(post(baseUrl + "/{id}/convert", lead.getId())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Студент"))
                .andExpect(jsonPath("$.lastName").value("Тестовый"))
                .andExpect(jsonPath("$.phone").value("+79998887766"))
                .andExpect(jsonPath("$.email").value("student@test.com"))
                .andExpect(jsonPath("$.birthDate").value("2000-01-01"))
                .andExpect(jsonPath("$.userId").value(Matchers.nullValue()));

        entityManager.flush();
        entityManager.clear();

        Lead updatedLead = leadRepository.findById(lead.getId()).orElseThrow();
        assertEquals(LeadStatus.CONVERTED_TO_STUDENT, updatedLead.getStatus());

        Student savedStudent = updatedLead.getConvertedStudent();
        assertNotNull(savedStudent);
        assertEquals("Студент", savedStudent.getFirstName());
        assertEquals("Тестовый", savedStudent.getLastName());
        assertEquals("+79998887766", savedStudent.getPhone());
        assertEquals("student@test.com", savedStudent.getEmail());
        assertEquals(LocalDate.of(2000, 1, 1), savedStudent.getBirthDate());
    }

    @Test
    @DisplayName("POST /{id}/convert — Успех (201) от ADMIN")
    @WithMockUser(roles = "ADMIN")
    void convertLead_Success_Admin() throws Exception {

        Lead lead = createLeadWithStatus(LeadStatus.NEW);

        LeadConvertRequest request = LeadConvertRequest.builder()
                .firstName("Студент")
                .lastName("Тестовый")
                .phone("+79998887766")
                .build();

        mockMvc.perform(post(baseUrl + "/{id}/convert", lead.getId())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Студент"))
                .andExpect(jsonPath("$.lastName").value("Тестовый"))
                .andExpect(jsonPath("$.phone").value("+79998887766"));

        entityManager.flush();
        entityManager.clear();

        Lead updatedLead = leadRepository.findById(lead.getId()).orElseThrow();
        assertEquals(LeadStatus.CONVERTED_TO_STUDENT, updatedLead.getStatus());
        assertNotNull(updatedLead.getConvertedStudent());
    }

    @Test
    @DisplayName("POST /{id}/convert — Ошибка 409 если уже конвертирован")
    @WithMockUser(roles = "MANAGER")
    void convertLead_AlreadyConverted_Returns409() throws Exception {

        Lead lead = createLeadWithStatus(LeadStatus.CONVERTED_TO_STUDENT);

        LeadConvertRequest request = LeadConvertRequest.builder()
                .firstName("Имя")
                .lastName("Фамилия")
                .phone("123")
                .build();

        mockMvc.perform(post(baseUrl + "/{id}/convert", lead.getId())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("conflict"));
    }

    @Test
    @DisplayName("POST /9999/convert — Лид не найден (404)")
    @WithMockUser(roles = "MANAGER")
    void convertLead_NotFound_Returns404() throws Exception {

        LeadConvertRequest request = LeadConvertRequest.builder()
                .firstName("Имя")
                .lastName("Фамилия")
                .phone("123")
                .build();

        mockMvc.perform(post(baseUrl + "/9999/convert")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("resource-not-found"));
    }

    @Test
    @DisplayName("POST /{id}/convert — Валидация (400) при пустом firstName")
    @WithMockUser(roles = "MANAGER")
    void convertLead_InvalidRequest_Returns400() throws Exception {

        Lead lead = createLeadWithStatus(LeadStatus.NEW);

        LeadConvertRequest request = LeadConvertRequest.builder()
                .firstName("")
                .lastName("Фамилия")
                .phone("+79990000000")
                .build();

        mockMvc.perform(post(baseUrl + "/{id}/convert", lead.getId())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("validation-error"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    @DisplayName("POST /{id}/convert — Доступ запрещен (403) для STUDENT")
    @WithMockUser(roles = "STUDENT")
    void convertLead_ForbiddenForStudent() throws Exception {

        LeadConvertRequest request = LeadConvertRequest.builder()
                .firstName("Ivan")
                .lastName("Petrov")
                .phone("+79990000000")
                .build();

        mockMvc.perform(post(baseUrl + "/1/convert")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /{id}/convert — Без токена (401)")
    @WithAnonymousUser
    void convertLead_UnauthorizedWithoutToken() throws Exception {

        Lead lead = createLeadWithStatus(LeadStatus.NEW);

        LeadConvertRequest request = LeadConvertRequest.builder()
                .firstName("Ivan")
                .lastName("Petrov")
                .phone("+79990000000")
                .build();

        mockMvc.perform(post(baseUrl + "/{id}/convert", lead.getId())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /{id}/convert — Только обязательные поля (201)")
    @WithMockUser(roles = "MANAGER")
    void convertLead_MinimalFields_Success() throws Exception {

        Lead lead = createLeadWithStatus(LeadStatus.NEW);

        LeadConvertRequest request = LeadConvertRequest.builder()
                .firstName("Min")
                .lastName("Max")
                .phone("555")
                .build();

        mockMvc.perform(post(baseUrl + "/{id}/convert", lead.getId())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Min"))
                .andExpect(jsonPath("$.lastName").value("Max"))
                .andExpect(jsonPath("$.phone").value("555"))
                .andExpect(jsonPath("$.userId").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.email").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.birthDate").value(Matchers.nullValue()));

        entityManager.flush();
        entityManager.clear();

        Student student = leadRepository.findById(lead.getId())
                .orElseThrow()
                .getConvertedStudent();

        assertNotNull(student);
        assertNull(student.getEmail());
        assertNull(student.getBirthDate());
    }

    @Test
    @DisplayName("POST /{id}/comments с валидным текстом от MANAGER — ответ 201, тело содержит корректные поля")
    @WithMockUser(username = "manager@test.com", roles = "MANAGER")
    void addComment_ValidRequest_Manager_ShouldReturn201() throws Exception {
        Lead lead = createLeadWithStatus(LeadStatus.NEW);

        String commentText = "Клиент заинтересован в курсе Java Backend. Договорились перезвонить в пятницу.";

        LeadCommentCreateRequest request = LeadCommentCreateRequest.builder()
                .text(commentText)
                .build();

        mockMvc.perform(post(baseUrl + "/{id}/comments", lead.getId())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.leadId").value(lead.getId().intValue()))
                .andExpect(jsonPath("$.author.id").exists())
                .andExpect(jsonPath("$.author.firstName").exists())
                .andExpect(jsonPath("$.author.lastName").exists())
                .andExpect(jsonPath("$.text").value(commentText))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("POST /{id}/comments от ADMIN — ответ 201, тело содержит корректные поля")
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void addComment_ValidRequest_Admin_ShouldReturn201() throws Exception {
        Lead lead = createLeadWithStatus(LeadStatus.NEW);

        String commentText = "Администратор подтверждает статус лида";

        LeadCommentCreateRequest request = LeadCommentCreateRequest.builder()
                .text(commentText)
                .build();

        mockMvc.perform(post(baseUrl + "/{id}/comments", lead.getId())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.leadId").value(lead.getId().intValue()))
                .andExpect(jsonPath("$.author.id").exists())
                .andExpect(jsonPath("$.author.firstName").exists())
                .andExpect(jsonPath("$.author.lastName").exists())
                .andExpect(jsonPath("$.text").value(commentText))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("POST /{id}/comments от пользователя с ролью STUDENT — ответ 403")
    @WithMockUser(username = "student@test.com", roles = "STUDENT")
    void addComment_StudentRole_ShouldReturn403() throws Exception {
        Lead lead = createLeadWithStatus(LeadStatus.NEW);

        String commentText = "Комментарий от студента";

        LeadCommentCreateRequest request = LeadCommentCreateRequest.builder()
                .text(commentText)
                .build();

        mockMvc.perform(post(baseUrl + "/{id}/comments", lead.getId())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /{id}/comments без токена — ответ 401")
    @WithAnonymousUser
    void addComment_WithoutToken_ShouldReturn401() throws Exception {
        Lead lead = createLeadWithStatus(LeadStatus.NEW);

        String commentText = "Комментарий без авторизации";

        LeadCommentCreateRequest request = LeadCommentCreateRequest.builder()
                .text(commentText)
                .build();

        mockMvc.perform(post(baseUrl + "/{id}/comments", lead.getId())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /{id}/comments с пустым text — ответ 400 с полем errors")
    @WithMockUser(username = "manager@test.com", roles = "MANAGER")
    void addComment_EmptyText_ShouldReturn400() throws Exception {
        Lead lead = createLeadWithStatus(LeadStatus.NEW);

        LeadCommentCreateRequest request = LeadCommentCreateRequest.builder()
                .text("   ")
                .build();

        mockMvc.perform(post(baseUrl + "/{id}/comments", lead.getId())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.type").value("internal-error"))
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    @DisplayName("POST /{id}/comments с text из пробелов — валидация срабатывает")
    @WithMockUser(username = "manager@test.com", roles = "MANAGER")
    void addComment_WhitespaceText_ShouldReturn400() throws Exception {
        Lead lead = createLeadWithStatus(LeadStatus.NEW);

        LeadCommentCreateRequest request = LeadCommentCreateRequest.builder()
                .text("   ")
                .build();

        mockMvc.perform(post(baseUrl + "/{id}/comments", lead.getId())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.type").value("internal-error"));
    }

    @Test
    @DisplayName("POST /9999/comments (несуществующий лид) — ответ 404")
    @WithMockUser(username = "manager@test.com", roles = "MANAGER")
    void addComment_LeadNotFound_ShouldReturn404() throws Exception {
        Long nonExistentLeadId = 9999L;

        LeadCommentCreateRequest request = LeadCommentCreateRequest.builder()
                .text("Комментарий к несуществующему лиду")
                .build();

        mockMvc.perform(post(baseUrl + "/{id}/comments", nonExistentLeadId)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("resource-not-found"))
                .andExpect(jsonPath("$.detail").value(containsString("Lead not found with id: 9999")))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /{id}/comments — проверка поля author: в ответе содержится информация о текущем аутентифицированном пользователе")
    @WithMockUser(username = "manager@test.com", roles = "MANAGER")
    void addComment_CheckAuthorField_ShouldReturnCurrentUser() throws Exception {
        Lead lead = createLeadWithStatus(LeadStatus.NEW);

        String commentText = "Тестовый комментарий для проверки автора";

        LeadCommentCreateRequest request = LeadCommentCreateRequest.builder()
                .text(commentText)
                .build();

        mockMvc.perform(post(baseUrl + "/{id}/comments", lead.getId())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.leadId").value(lead.getId().intValue()))
                .andExpect(jsonPath("$.author.id").exists())
                .andExpect(jsonPath("$.author.firstName").exists())
                .andExpect(jsonPath("$.author.lastName").exists())
                .andExpect(jsonPath("$.author.firstName").value("Manager"))
                .andExpect(jsonPath("$.author.lastName").value("Test"))
                .andExpect(jsonPath("$.text").value(commentText))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("POST /{id}/comments — проверка поля createdAt: значение устанавливается автоматически и близко к текущему времени")
    @WithMockUser(username = "manager@test.com", roles = "MANAGER")
    void addComment_CheckCreatedAtField_ShouldBeCloseToCurrentTime() throws Exception {

        Lead lead = createLeadWithStatus(LeadStatus.NEW);

        String commentText = "Тестовый комментарий для проверки createdAt";

        LeadCommentCreateRequest request = LeadCommentCreateRequest.builder()
                .text(commentText)
                .build();

        LocalDateTime beforeRequest = LocalDateTime.now();

        MvcResult result = mockMvc.perform(post(baseUrl + "/{id}/comments", lead.getId())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.leadId").value(lead.getId().intValue()))
                .andExpect(jsonPath("$.author.id").exists())
                .andExpect(jsonPath("$.author.firstName").exists())
                .andExpect(jsonPath("$.author.lastName").exists())
                .andExpect(jsonPath("$.text").value(commentText))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn();

        LocalDateTime afterRequest = LocalDateTime.now();

        String responseJson = result.getResponse().getContentAsString();
        LeadCommentResponse response = objectMapper.readValue(responseJson, LeadCommentResponse.class);

        assertNotNull(response.getCreatedAt());
        assertTrue(response.getCreatedAt().isAfter(beforeRequest) || response.getCreatedAt().isEqual(beforeRequest));
        assertTrue(response.getCreatedAt().isBefore(afterRequest) || response.getCreatedAt().isEqual(afterRequest));
    }


    @Test
    @DisplayName("Тест 1: обновление данных работает → HTTP 200")
    @WithMockUser(roles = "MANAGER")
    void updateLead_WithValidData_ShouldReturn200() throws Exception {

        Lead lead = new Lead();
        lead.setFirstName("Иван");
        lead.setLastName("Петров");
        lead.setPhone("+79161111111");
        lead.setEmail("ivan@mail.ru");
        lead.setSource("Сайт");
        lead.setStatus(LeadStatus.NEW);
        lead.setComment("Старый комментарий");
        Lead savedLead = leadRepository.saveAndFlush(lead);

        LeadUpdateRequest updateRequest = new LeadUpdateRequest();
        updateRequest.setFirstName("Алексей");
        updateRequest.setLastName("Смирнов");
        updateRequest.setPhone("+79162222222");
        updateRequest.setEmail("alexey@mail.ru");
        updateRequest.setSource("Реклама");
        updateRequest.setComment("Новый комментарий");
        updateRequest.setInterestedCourseId(null);
        updateRequest.setAssignedManagerId(null);

        mockMvc.perform(put(baseUrl + "/{id}", savedLead.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+79162222222"))
                .andExpect(jsonPath("$.firstName").value("Алексей"))
                .andExpect(jsonPath("$.lastName").value("Смирнов"))
                .andExpect(jsonPath("$.email").value("alexey@mail.ru"))
                .andExpect(jsonPath("$.source").value("Реклама"))
                .andExpect(jsonPath("$.comment").value("Новый комментарий"))
                .andExpect(jsonPath("$.status").value(LeadStatus.NEW.name()));

        Lead updatedLead = leadRepository.findById(savedLead.getId()).orElseThrow();
        assertThat(updatedLead.getPhone()).isEqualTo("+79162222222");
        assertThat(updatedLead.getStatus()).isEqualTo(LeadStatus.NEW);
    }

    @Test
    @DisplayName("Тест 2: конвертированный лид — обновление заблокировано → HTTP 400")
    @WithMockUser(roles = "MANAGER")
    void updateLead_WithConvertedStatus_ShouldReturn400() throws Exception {

        Lead lead = new Lead();
        lead.setFirstName("Иван");
        lead.setLastName("Петров");
        lead.setPhone("+79161111111");
        lead.setEmail("ivan@mail.ru");
        lead.setSource("Сайт");
        lead.setStatus(LeadStatus.CONVERTED_TO_STUDENT);
        lead.setComment("Конвертирован в студента");
        Lead savedLead = leadRepository.saveAndFlush(lead);

        LeadUpdateRequest updateRequest = new LeadUpdateRequest();
        updateRequest.setFirstName("Алексей");
        updateRequest.setPhone("+79162222222");

        mockMvc.perform(put(baseUrl + "/{id}", savedLead.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("bad-request"))
                .andExpect(jsonPath("$.detail").value("Нельзя редактировать конвертированного лида"));

        Lead unchangedLead = leadRepository.findById(savedLead.getId()).orElseThrow();
        assertThat(unchangedLead.getPhone()).isEqualTo("+79161111111");
        assertThat(unchangedLead.getStatus()).isEqualTo(LeadStatus.CONVERTED_TO_STUDENT);
    }

    @Test
    @DisplayName("Тест 3: лид не найден → HTTP 404")
    @WithMockUser(roles = "MANAGER")
    void updateLead_WithNonExistentId_ShouldReturn404() throws Exception {
        Long nonExistentId = 9999L;

        LeadUpdateRequest updateRequest = new LeadUpdateRequest();
        updateRequest.setFirstName("Алексей");
        updateRequest.setPhone("+79162222222");

        mockMvc.perform(put(baseUrl + "/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("resource-not-found"))
                .andExpect(jsonPath("$.detail").value("Lead с id " + nonExistentId + " не найден"));
    }

    @Test
    @DisplayName("Тест 4: пользователь с ролью TEACHER → HTTP 403")
    @WithMockUser(roles = "TEACHER")
    void updateLead_WithTeacherRole_ShouldReturn403() throws Exception {

        Lead lead = new Lead();
        lead.setFirstName("Иван");
        lead.setLastName("Петров");
        lead.setPhone("+79161111111");
        lead.setEmail("ivan@mail.ru");
        lead.setSource("Сайт");
        lead.setStatus(LeadStatus.NEW);
        Lead savedLead = leadRepository.saveAndFlush(lead);

        LeadUpdateRequest updateRequest = new LeadUpdateRequest();
        updateRequest.setFirstName("Алексей");
        updateRequest.setPhone("+79162222222");

        mockMvc.perform(put(baseUrl + "/{id}", savedLead.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());

        Lead unchangedLead = leadRepository.findById(savedLead.getId()).orElseThrow();
        assertThat(unchangedLead.getPhone()).isEqualTo("+79161111111");
        assertThat(unchangedLead.getStatus()).isEqualTo(LeadStatus.NEW);
    }
    @Test
    @DisplayName("PATCH /{id}/status с валидным статусом от пользователя с ролью MANAGER — ответ 200, тело содержит обновлённый статус")
    void changeStatus_ValidStatus_Manager_ShouldReturn200() throws Exception {
        Lead lead = createLeadWithStatus(LeadStatus.NEW);
        LeadStatusUpdateRequest request = new LeadStatusUpdateRequest(LeadStatus.IN_PROGRESS);

        mockMvc.perform(patch(baseUrl + "/{id}/status", lead.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lead.getId().intValue()))
                .andExpect(jsonPath("$.status").value(LeadStatus.IN_PROGRESS.name()));
    }

    @Test
    @DisplayName("PATCH /{id}/status от пользователя с ролью ADMIN — ответ 200")
    void changeStatus_ValidStatus_Admin_ShouldReturn200() throws Exception {
        Lead lead = createLeadWithStatus(LeadStatus.NEW);
        LeadStatusUpdateRequest request = new LeadStatusUpdateRequest(LeadStatus.CONTACTED);

        mockMvc.perform(patch(baseUrl + "/{id}/status", lead.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lead.getId().intValue()))
                .andExpect(jsonPath("$.status").value(LeadStatus.CONTACTED.name()));
    }

    @Test
    @DisplayName("PATCH /{id}/status от пользователя с ролью STUDENT — ответ 403")
    @WithMockUser(roles = "STUDENT")
    void changeStatus_StudentRole_ShouldReturn403() throws Exception {
        Lead lead = createLeadWithStatus(LeadStatus.NEW);
        LeadStatusUpdateRequest request = new LeadStatusUpdateRequest(LeadStatus.IN_PROGRESS);

        mockMvc.perform(patch(baseUrl + "/{id}/status", lead.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /{id}/status без токена — ответ 401")
    void changeStatus_WithoutToken_ShouldReturn401() throws Exception {
        Lead lead = createLeadWithStatus(LeadStatus.NEW);
        LeadStatusUpdateRequest request = new LeadStatusUpdateRequest(LeadStatus.IN_PROGRESS);

        mockMvc.perform(patch(baseUrl + "/{id}/status", lead.getId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext(
                                org.springframework.security.core.context.SecurityContextHolder.createEmptyContext()
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /{id}/status с status: \"CONVERTED_TO_STUDENT\" — ответ 409")
    void changeStatus_ToConvertedToStudentDirectly_ShouldReturn409() throws Exception {
        Lead lead = createLeadWithStatus(LeadStatus.NEW);
        LeadStatusUpdateRequest request = new LeadStatusUpdateRequest(LeadStatus.CONVERTED_TO_STUDENT);

        mockMvc.perform(patch(baseUrl + "/{id}/status", lead.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("conflict"));
    }

    @Test
    @DisplayName("PATCH /9999/status — ответ 404")
    void changeStatus_LeadNotFound_ShouldReturn404() throws Exception {
        LeadStatusUpdateRequest request = new LeadStatusUpdateRequest(LeadStatus.IN_PROGRESS);

        mockMvc.perform(patch(baseUrl + "/9999/status")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("resource-not-found"));
    }

    @Test
    @DisplayName("PATCH /{id}/status с телом {} — ответ 400 с полем errors")
    void changeStatus_EmptyBody_ShouldReturn400() throws Exception {
        Lead lead = createLeadWithStatus(LeadStatus.NEW);

        mockMvc.perform(patch(baseUrl + "/{id}/status", lead.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @DisplayName("PATCH /{id}/status с неизвестным значением статуса (строка) — ответ 400")
    void changeStatus_InvalidEnumString_ShouldReturn400() throws Exception {
        Lead lead = createLeadWithStatus(LeadStatus.NEW);
        String invalidJson = "{\"status\": \"UNKNOWN_LEAD_STATUS_VALUE\"}";

        mockMvc.perform(patch(baseUrl + "/{id}/status", lead.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("bad-request"));
    }



    @Test
    @DisplayName("GET /{id}/comments с существующим лидом, у которого есть комментарии — ответ 200, список не пуст, порядок по createdAt ASC")
    @WithMockUser(username = "manager@test.com", roles = "MANAGER")
    void getComments_WithExistingLeadAndComments_ShouldReturn200WithCommentsSortedByCreatedAt() throws Exception {
        // Arrange
        Lead lead = createLeadWithStatus(LeadStatus.NEW);
        lead = leadRepository.save(lead);

        // Создаём комментарии с фиксированными временными метками (более надежно, чем sleep)
        Instant firstCommentTime = Instant.now().minusSeconds(2);
        Instant secondCommentTime = Instant.now().minusSeconds(1);
        Instant thirdCommentTime = Instant.now();

        // Используем mockMvc для добавления комментариев
        mockMvc.perform(post(baseUrl + "/{id}/comments", lead.getId())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(LeadCommentCreateRequest.builder()
                                .text("привет")
                                .build())))
                .andExpect(status().isCreated());

        mockMvc.perform(post(baseUrl + "/{id}/comments", lead.getId())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(LeadCommentCreateRequest.builder()
                                .text("Second comment")
                                .build())))
                .andExpect(status().isCreated());

        mockMvc.perform(post(baseUrl + "/{id}/comments", lead.getId())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(LeadCommentCreateRequest.builder()
                                .text("Third comment")
                                .build())))
                .andExpect(status().isCreated());

        // Act
        MvcResult result = mockMvc.perform(get(baseUrl + "/{id}/comments", lead.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Проверяем сырые данные ответа
        String responseJson = new String(
                result.getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8
        );

        // Проверяем, что JSON валиден и содержит ожидаемую структуру
        assertFalse(responseJson.isEmpty(), "Ответ пустой");
        assertTrue(responseJson.startsWith("["), "Ответ не является массивом JSON");
        assertTrue(responseJson.endsWith("]"), "Ответ не завершается правильно");

        // Десериализуем JSON в объект
        List<LeadCommentResponse> comments = objectMapper.readValue(
                responseJson,
                new TypeReference<List<LeadCommentResponse>>() {
                }
        );

        // Assert
        assertEquals(3, comments.size(), "Ожидалось 3 комментария");

        // Проверяем порядок по createdAt (ASC)
        for (int i = 0; i < comments.size() - 1; i++) {
            assertTrue(
                    comments.get(i).getCreatedAt().isBefore(comments.get(i + 1).getCreatedAt()),
                    String.format(
                            "Comment %d (%s) должен быть раньше комментария %d (%s)",
                            i, comments.get(i).getText(),
                            i + 1, comments.get(i + 1).getText()
                    )
            );
        }

        // Проверяем содержимое комментариев
        assertEquals("привет", comments.get(0).getText());
        assertEquals("Second comment", comments.get(1).getText());
        assertEquals("Third comment", comments.get(2).getText());

        // Проверяем, что все поля существуют и корректны
        for (LeadCommentResponse comment : comments) {
            assertNotNull(comment.getAuthor(), "Автор комментария не найден");
            assertNotNull(comment.getAuthor().getFirstName(), "Имя автора отсутствует");
            assertNotNull(comment.getAuthor().getLastName(), "Фамилия автора отсутствует");
            assertNotNull(comment.getCreatedAt(), "Время создания комментария отсутствует");
        }
    }

    @Test
    @DisplayName("GET /{id}/comments с существующим лидом без комментариев — ответ 200, тело []")
    @WithMockUser(username = "manager@test.com", roles = "MANAGER")
    void getComments_WithExistingLeadNoComments_ShouldReturn200WithEmptyList() throws Exception {
        // Arrange
        Lead lead = createLeadWithStatus(LeadStatus.NEW);

        // Act & Assert
        mockMvc.perform(get(baseUrl + "/{id}/comments", lead.getId())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(0)));
    }

    @Test
    @DisplayName("GET /{id}/comments от пользователя с ролью ADMIN — ответ 200")
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void getComments_WithAdminRole_ShouldReturn200() throws Exception {
        // Arrange
        Lead lead = createLeadWithStatus(LeadStatus.NEW);
        // Добавим комментарий, чтобы не возвращался пустой список
        mockMvc.perform(post(baseUrl + "/{id}/comments", lead.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(LeadCommentCreateRequest.builder()
                        .text("Тестовый комментарий")
                        .build())));

        // Act & Assert
        mockMvc.perform(get(baseUrl + "/{id}/comments", lead.getId())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(1)));
    }

    @Test
    @DisplayName("GET /{id}/comments от пользователя с ролью MANAGER — ответ 200")
    @WithMockUser(username = "manager@test.com", roles = "MANAGER")
    void getComments_WithManagerRole_ShouldReturn200() throws Exception {
        // Arrange
        Lead lead = createLeadWithStatus(LeadStatus.NEW);
        // Добавим комментарий, чтобы не возвращался пустой список
        mockMvc.perform(post(baseUrl + "/{id}/comments", lead.getId())
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(LeadCommentCreateRequest.builder()
                        .text("Тестовый комментарий")
                        .build())));

        // Act & Assert
        mockMvc.perform(get(baseUrl + "/{id}/comments", lead.getId())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(1)));
    }

    @Test
    @DisplayName("GET /{id}/comments от пользователя с ролью STUDENT — ответ 403")
    @WithMockUser(username = "student@test.com", roles = "STUDENT")
    void getComments_WithStudentRole_ShouldReturn403() throws Exception {
        // Arrange
        Lead lead = createLeadWithStatus(LeadStatus.NEW);

        // Act & Assert
        mockMvc.perform(get(baseUrl + "/{id}/comments", lead.getId())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /{id}/comments без токена — ответ 401")
    @WithAnonymousUser
    void getComments_WithoutToken_ShouldReturn401() throws Exception {
        // Arrange
        Lead lead = createLeadWithStatus(LeadStatus.NEW);

        // Act & Assert
        mockMvc.perform(get(baseUrl + "/{id}/comments", lead.getId())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /9999/comments (несуществующий лид) — ответ 404")
    @WithMockUser(username = "manager@test.com", roles = "MANAGER")
    void getComments_WithNonExistingLead_ShouldReturn404() throws Exception {
        Long nonExistentLeadId = 9999L;

        mockMvc.perform(get(baseUrl + "/{id}/comments", nonExistentLeadId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("resource-not-found"));
    }


    @Test
    @DisplayName("GET /{id}/comments — проверка корректности полей ответа: leadId, author.id, author.firstName, text, createdAt")
    @WithMockUser(username = "manager@test.com", roles = "MANAGER")
    void getComments_CheckResponseFields() throws Exception {
        // Arrange
        Lead lead = createLeadWithStatus(LeadStatus.NEW);
        lead = leadRepository.save(lead);

        // Используем кириллический текст для проверки UTF-8
        String commentText = "Тестовый комментарий для проверки полей ответа с кириллицей";

        // Добавляем комментарий с явным указанием кодировки в контенте
        mockMvc.perform(post(baseUrl + "/{id}/comments", lead.getId())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .characterEncoding("UTF-8")
                        .content(objectMapper.writeValueAsString(
                                LeadCommentCreateRequest.builder()
                                        .text(commentText)
                                        .build()
                        )))
                .andExpect(status().isCreated());

        // Act
        MvcResult result = mockMvc.perform(get(baseUrl + "/{id}/comments", lead.getId())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8"))
                .andExpect(status().isOk())
                .andReturn();

        // Проверяем сырые данные ответа (для отладки)
        String responseJson = new String(
                result.getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8
        );


        // Десериализуем JSON в объект с явным указанием кодировки
        List<LeadCommentResponse> comments = objectMapper.readValue(
                responseJson,
                new TypeReference<List<LeadCommentResponse>>() {
                }
        );

        // Assert
        assertFalse(comments.isEmpty(), "Список комментариев не должен быть пустым");

        LeadCommentResponse comment = comments.get(0);

        // Проверка leadId
        assertEquals(lead.getId().intValue(), comment.getLeadId(),
                "leadId должен соответствовать ID лида");

        // Проверка автора
        assertNotNull(comment.getAuthor(), "Автор комментария не должен быть null");
        assertNotNull(comment.getAuthor().getId(), "ID автора не должен быть null");

        // Проверка имени и фамилии автора (должны совпадать с текущим пользователем)
        assertEquals(testManager.getFirstName(), comment.getAuthor().getFirstName(),
                "Имя автора должно совпадать с текущим пользователем");
        assertEquals(testManager.getLastName(), comment.getAuthor().getLastName(),
                "Фамилия автора должна совпадать с текущим пользователем");

        // Основная проверка текста комментария с кириллицей
        assertEquals(commentText, comment.getText(),
                "Текст комментария должен сохраняться корректно (UTF-8)");

        // Проверка createdAt
        assertNotNull(comment.getCreatedAt(), "createdAt не должен быть null");
        assertDoesNotThrow(() -> {
            LocalDateTime now = LocalDateTime.now();
            assertTrue(comment.getCreatedAt().isBefore(now.plusHours(1)),
                    "createdAt не должен быть в будущем");
            assertTrue(comment.getCreatedAt().isAfter(now.minusDays(1)),
                    "createdAt не должен быть слишком давно");
        }, "createdAt должен быть валидной датой");

        // Дополнительная проверка кодировки (если есть подозрения на проблемы)
        byte[] textBytes = comment.getText().getBytes(StandardCharsets.UTF_8);
        String restoredText = new String(textBytes, StandardCharsets.UTF_8);
        assertEquals(commentText, restoredText,
                "Кодировка текста комментария должна оставаться UTF-8");
    }
}
