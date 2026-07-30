package com.covenantcode.crm.controller;

import com.covenantcode.crm.BaseIntegrationTest;
import com.covenantcode.crm.dto.auth.LoginRequest;
import com.covenantcode.crm.dto.auth.RegisterRequest;
import com.covenantcode.crm.entity.Role;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.repository.RoleRepository;
import com.covenantcode.crm.repository.UserRepository;
import com.covenantcode.crm.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class AuthControllerIntegrationTest extends BaseIntegrationTest {
    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL = "/api/v1/auth/login";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Role testRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        testRole = roleRepository.findByName(RoleName.MANAGER)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(RoleName.MANAGER);
                    return roleRepository.save(role);
                });

        testUser = User.builder()
                .firstName("Тест")
                .lastName("Пользователь")
                .email("login_test@example.com")
                .password(passwordEncoder.encode("test_password_123"))
                .role(testRole)
                .enabled(true)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    public void register_success() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Ivan",
                "Ivanov",
                "ivan@example.com",
                "password123",
                "+79991234567"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("MANAGER"));
        User user = userRepository.findByEmail("ivan@example.com").orElseThrow();

        org.junit.jupiter.api.Assertions.assertNotEquals("password123", user.getPassword());
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("password123", user.getPassword()));
    }

    @Test
    public void register_conflict_sameEmail() throws Exception {
        Role role = roleRepository.findByName(RoleName.MANAGER).orElseThrow();

        User existing = new User();
        existing.setFirstName("Alex");
        existing.setLastName("Stone");
        existing.setEmail("busy@example.com");
        existing.setPassword(passwordEncoder.encode("encoded-pass"));
        existing.setRole(role);
        userRepository.save(existing);

        RegisterRequest request = new RegisterRequest(
                "New",
                "User",
                "busy@example.com",
                "password123",
                null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.type").value("conflict"));
    }

    @Test
    public void register_validationError() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "",
                "Ivanov",
                "not-an-email",
                "password123",
                null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[?(@.field == 'email' && @.message == 'Некорректный формат email')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'firstName' && @.message == 'Имя обязательно')]").exists());
    }

    @Test
    @DisplayName("Успешный вход с корректными учетными данными")
    void testControllerIntegrationLoginSuccess() throws Exception {
        LoginRequest request = new LoginRequest(
                testUser.getEmail(),
                "test_password_123"
        );

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    @DisplayName("Ошибка логина: неверный пароль")
    void testControllerIntegrationLoginWrongPassword() throws Exception {
        LoginRequest request = new LoginRequest(
                testUser.getEmail(),
                "wrong_password"
        );

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Неверный email или пароль"));
    }

    @Test
    @DisplayName("Ошибка логина: неверный email")
    void testControllerIntegrationLoginWrongEmail() throws Exception {
        LoginRequest request = new LoginRequest(
                "wrong@example.com",
                "test_password_123"
        );

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Неверный email или пароль"));
    }

    @Test
    @DisplayName("Ошибка логина: пустое поле email")
    void testControllerIntegrationLoginEmptyEmail() throws Exception {
        LoginRequest request = new LoginRequest(
                "",
                "test_password_123"
        );

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("validation-error"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Ошибка логина: пустое поле пароля")
    void testControllerIntegrationLoginEemptyPassword() throws Exception {
        LoginRequest request = new LoginRequest(
                testUser.getEmail(),
                ""
        );

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("validation-error"))
                .andExpect(jsonPath("$.status").value(400));
    }
}
