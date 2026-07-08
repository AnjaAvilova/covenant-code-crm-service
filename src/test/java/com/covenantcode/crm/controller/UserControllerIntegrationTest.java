package com.covenantcode.crm.controller;

import com.covenantcode.crm.BaseIntegrationTest;
import com.covenantcode.crm.entity.Role;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.repository.RoleRepository;
import com.covenantcode.crm.repository.StudyGroupRepository;
import com.covenantcode.crm.repository.UserRepository;
import com.covenantcode.crm.security.JwtService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private StudyGroupRepository studyGroupRepository;

    private String adminToken;
    private String managerToken;

    private User adminUser;
    private User managerUser;

    @BeforeEach
    void setUp() {

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(RoleName.ADMIN);
                    return roleRepository.save(role);
                });

        Role managerRole = roleRepository.findByName(RoleName.MANAGER)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(RoleName.MANAGER);
                    return roleRepository.save(role);
                });

        adminUser = User.builder()
                .firstName("Admin")
                .lastName("User")
                .email("admin@test.ru")
                .password(passwordEncoder.encode("password123"))
                .role(adminRole)
                .enabled(true)
                .build();
        adminUser = userRepository.save(adminUser);

        managerUser = User.builder()
                .firstName("Manager")
                .lastName("User")
                .email("manager@test.ru")
                .password(passwordEncoder.encode("password123"))
                .role(managerRole)
                .enabled(true)
                .build();
        managerUser = userRepository.save(managerUser);

        adminToken = jwtService.generateToken(adminUser);
        managerToken = jwtService.generateToken(managerUser);
    }

    @AfterEach
    void tearDown() {
        studyGroupRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void getAllUsers_withAdminToken_success() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .param("page", "0")
                        .param("size", "5")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    public void getAllUsers_withManagerToken_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getAllUsers_withoutToken_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN получает профиль другого пользователя — HTTP 200")
    void getUserById_adminGetsAnotherProfile_success() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", managerUser.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(managerUser.getId()))
                .andExpect(jsonPath("$.email").value(managerUser.getEmail()));
    }

    @Test
    @DisplayName("Пользователь получает свой профиль — HTTP 200")
    void getUserById_userGetsOwnProfile_success() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", managerUser.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(managerUser.getId()))
                .andExpect(jsonPath("$.email").value(managerUser.getEmail()));
    }

    @Test
    @DisplayName("MANAGER запрашивает чужой профиль — HTTP 403")
    void getUserById_managerGetsAnotherProfile_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", adminUser.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("forbidden"));
    }

    @Test
    @DisplayName("Несуществующий ID (ADMIN) — HTTP 404")
    void getUserById_adminGetsMissingProfile_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", 999L)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("resource-not-found"));
    }

    @Test
    void adminBlocksAnotherUser_shouldReturn200AndEnabledFalse() throws Exception {
        User manager = userRepository.findByEmail("manager@test.ru").orElseThrow();
        long managerId = manager.getId();

        mockMvc.perform(patch("/api/v1/users/{id}/enabled", managerId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(managerId))
                .andExpect(jsonPath("$.enabled").value(false));

        User updatedManager = userRepository.findById(managerId).orElseThrow();
        assertThat(updatedManager.isEnabled()).isFalse();
    }

    @Test
    void adminUnblocksUser_shouldReturn200AndEnabledTrue() throws Exception {
        User manager = userRepository.findByEmail("manager@test.ru").orElseThrow();
        long managerId = manager.getId();

        mockMvc.perform(patch("/api/v1/users/{id}/enabled", managerId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        User updatedManager = userRepository.findById(managerId).orElseThrow();
        assertThat(updatedManager.isEnabled()).isTrue();
    }

    @Test
    void adminBlocksHimself_shouldReturnBadRequest() throws Exception {
        User admin = userRepository.findByEmail("admin@test.ru").orElseThrow();
        long adminId = admin.getId();

        mockMvc.perform(patch("/api/v1/users/{id}/enabled", adminId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("bad-request"))
                .andExpect(jsonPath("$.detail").value("Нельзя заблокировать собственный аккаунт"));
    }

    @Test
    void managerTriesToBlockUser_shouldReturnForbidden() throws Exception {
        User manager = userRepository.findByEmail("manager@test.ru").orElseThrow();
        mockMvc.perform(patch("/api/v1/users/1/enabled")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminBlocksNonExistentUser_shouldReturnNotFound() throws Exception {
        long nonExistentId = 999L;

        mockMvc.perform(patch("/api/v1/users/{id}/enabled", nonExistentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": false}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("resource-not-found"));
    }

    @Test
    void getAllUsers_searchByName_findsMatch() throws Exception {
        Role managerRole = roleRepository.findByName(RoleName.MANAGER).orElseThrow();

        User ivan = User.builder()
                .firstName("Иван")
                .lastName("Петров")
                .email("ivan.petrov@test.ru")
                .password(passwordEncoder.encode("password123"))
                .role(managerRole)
                .enabled(true)
                .build();
        userRepository.save(ivan);

        User alina = User.builder()
                .firstName("Алина")
                .lastName("Смирнова")
                .email("alina.smirnova@test.ru")
                .password(passwordEncoder.encode("password123"))
                .role(managerRole)
                .enabled(true)
                .build();
        userRepository.save(alina);

        mockMvc.perform(get("/api/v1/users")
                        .param("search", "иван")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Иван"));
    }

    @Test
    void getAllUsers_searchNoResults_emptyList() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .param("search", "zzznobody")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getAllUsers_withoutSearchParam_returnsAllUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(2)));
    }
}
