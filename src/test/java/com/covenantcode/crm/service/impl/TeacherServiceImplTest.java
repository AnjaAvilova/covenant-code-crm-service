package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.teacher.TeacherCreateRequest;
import com.covenantcode.crm.dto.teacher.TeacherResponse;
import com.covenantcode.crm.entity.Role;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.exception.ConflictException;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.mapper.TeacherMapper;
import com.covenantcode.crm.repository.RoleRepository;
import com.covenantcode.crm.repository.UserRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
//import static org.assertj.core.api.Assertions;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class TeacherServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeacherMapper teacherMapper;

    @InjectMocks
    private TeacherServiceImpl teacherService;

    @Captor
    private ArgumentCaptor<Specification<User>> specCaptor;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private TeacherResponse testResponse;
    private Pageable defaultPageable;
    private TeacherCreateRequest request;


    @BeforeEach
    void setUp() {
        Role teacherRole = new Role();
        teacherRole.setId(1L);
        teacherRole.setName(RoleName.TEACHER);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        testUser = User.builder()
                .id(1L)
                .firstName("Иван")
                .lastName("Петров")
                .email("ivan.petrov@example.com")
                .phone("+79161234567")
                .enabled(true)
                .password("encoded_password")
                .role(teacherRole)
                .createdAt(now)
                .updatedAt(now)
                .build();

        request = TeacherCreateRequest.builder()
                .firstName("Иван")
                .lastName("Петров")
                .email("ivan.petrov@example.com")
                .password("secure1234")
                .phone("+79161234567")
                .build();

        testResponse = TeacherResponse.builder()
                .id(1L)
                .firstName("Иван")
                .lastName("Петров")
                .email("ivan.petrov@example.com")
                .phone("+79161234567")
                .enabled(true)
                .createdAt(now.toLocalDateTime())
                .build();

        defaultPageable = PageRequest.of(0, 20, Sort.by("lastName"));
    }

    @Test
    @DisplayName("getAll без search — фильтрует только по роли TEACHER")
    void getAll_withoutSearch_filtersByTeacherRole() {
        Page<User> userPage = new PageImpl<>(List.of(testUser), defaultPageable, 1);

        when(userRepository.findAll(any(Specification.class), eq(defaultPageable)))
                .thenReturn(userPage);
        when(teacherMapper.toResponse(testUser)).thenReturn(testResponse);

        Page<TeacherResponse> result = teacherService.getAll(null, defaultPageable);

        assertThat(result.getContent())
                .hasSize(1)
                .containsExactly(testResponse);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);

        verify(userRepository).findAll(specCaptor.capture(), eq(defaultPageable));

        Specification<User> capturedSpec = specCaptor.getValue();
        assertThat(capturedSpec).isNotNull();
        assertRoleFilterApplied(capturedSpec);

        verify(teacherMapper, times(1)).toResponse(testUser);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void assertRoleFilterApplied(Specification<User> spec) {
        Root<User> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Path<Object> userRolePath = mock(Path.class);
        Path<RoleName> roleNamePath = mock(Path.class);

        when(root.get("role")).thenReturn(userRolePath);
        when(userRolePath.<RoleName>get("name")).thenReturn(roleNamePath);

        Predicate rolePredicate = mock(Predicate.class);
        when(cb.equal(roleNamePath, RoleName.TEACHER)).thenReturn(rolePredicate);

        Predicate result = spec.toPredicate((Root) root, query, cb);

        assertThat(result).isEqualTo(rolePredicate);

        verify(cb).equal(roleNamePath, RoleName.TEACHER);
    }

    @Test
    @DisplayName("getAll с search — фильтрует по роли TEACHER И по тексту в firstName/lastName/email")
    void getAll_withSearch_filtersByRoleAndText() {
        String search = "алекс";
        Pageable pageable = PageRequest.of(0, 20, Sort.by("lastName"));

        Role teacherRole = Role.builder()
                .id(1L)
                .name(RoleName.TEACHER)
                .build();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        User user = User.builder()
                .id(2L)
                .firstName("Aleksey")
                .lastName("Sidorov")
                .email("aleksey.sidorov@example.com")
                .phone("+79161234568")
                .enabled(true)
                .password("encoded_password")
                .role(teacherRole)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);

        TeacherResponse response = TeacherResponse.builder()
                .id(2L)
                .firstName("Aleksey")
                .lastName("Sidorov")
                .email("aleksey.sidorov@example.com")
                .phone("+79161234568")
                .enabled(true)
                .createdAt(now.toLocalDateTime())
                .build();

        when(userRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(userPage);
        when(teacherMapper.toResponse(user)).thenReturn(response);

        Page<TeacherResponse> result = teacherService.getAll(search, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(response);

        verify(userRepository).findAll(specCaptor.capture(), eq(pageable));
        verify(teacherMapper).toResponse(user);

        Specification<User> capturedSpec = specCaptor.getValue();
        assertThat(capturedSpec).isNotNull();

        assertRoleAndTextFilterApplied(capturedSpec, search);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void assertRoleAndTextFilterApplied(Specification<User> spec, String search) {
        Root<User> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Path<Object> userRolePath = mock(Path.class);
        Path<RoleName> roleNamePath = mock(Path.class);

        when(root.get("role")).thenReturn(userRolePath);
        when(userRolePath.<RoleName>get("name")).thenReturn(roleNamePath);

        Predicate rolePredicate = mock(Predicate.class);
        when(cb.equal(roleNamePath, RoleName.TEACHER)).thenReturn(rolePredicate);

        Path<String> firstNamePath = mock(Path.class);
        Path<String> lastNamePath = mock(Path.class);
        Path<String> emailPath = mock(Path.class);

        Expression<String> firstNameLower = mock(Expression.class);
        Expression<String> lastNameLower = mock(Expression.class);
        Expression<String> emailLower = mock(Expression.class);

        doReturn(firstNamePath).when(root).get("firstName");
        doReturn(lastNamePath).when(root).get("lastName");
        doReturn(emailPath).when(root).get("email");

        doReturn(firstNameLower).when(cb).lower(firstNamePath);
        doReturn(lastNameLower).when(cb).lower(lastNamePath);
        doReturn(emailLower).when(cb).lower(emailPath);

        String expectedPattern = "%" + search.trim().toLowerCase() + "%";

        Predicate firstNameLike = mock(Predicate.class);
        Predicate lastNameLike = mock(Predicate.class);
        Predicate emailLike = mock(Predicate.class);

        doReturn(firstNameLike).when(cb).like(firstNameLower, expectedPattern);
        doReturn(lastNameLike).when(cb).like(lastNameLower, expectedPattern);
        doReturn(emailLike).when(cb).like(emailLower, expectedPattern);

        Predicate textPredicate = mock(Predicate.class);
        doReturn(textPredicate).when(cb).or(firstNameLike, lastNameLike, emailLike);

        Predicate combinedPredicate = mock(Predicate.class);
        doReturn(combinedPredicate).when(cb).and(rolePredicate, textPredicate);

        Predicate result = spec.toPredicate(root, query, cb);

        assertThat(result).isEqualTo(combinedPredicate);

        verify(cb).equal(roleNamePath, RoleName.TEACHER);

        verify(cb).lower(firstNamePath);
        verify(cb).lower(lastNamePath);
        verify(cb).lower(emailPath);

        verify(cb).like(firstNameLower, expectedPattern);
        verify(cb).like(lastNameLower, expectedPattern);
        verify(cb).like(emailLower, expectedPattern);

        verify(cb).or(firstNameLike, lastNameLike, emailLike);
        verify(cb).and(rolePredicate, textPredicate);
    }

    @Test
    @DisplayName("create — успешное создание преподавателя")
    void create_success_returnsTeacherResponse() {
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleName.TEACHER)).thenReturn(Optional.of(testUser.getRole()));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(testUser);
        when(teacherMapper.toResponse(testUser)).thenReturn(testResponse);

        TeacherResponse result = teacherService.create(request);

        assertThat(result).isEqualTo(testResponse);
        verify(passwordEncoder).encode(request.getPassword());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User capturedUser = userCaptor.getValue();

        assertThat(capturedUser.getFirstName()).isEqualTo(request.getFirstName());
        assertThat(capturedUser.getLastName()).isEqualTo(request.getLastName());
        assertThat(capturedUser.getEmail()).isEqualTo(request.getEmail());
        assertThat(capturedUser.getPhone()).isEqualTo(request.getPhone());
        assertThat(capturedUser.getPassword()).isEqualTo("encodedPassword");
        assertThat(capturedUser.isEnabled()).isTrue();
        assertThat(capturedUser.getRole()).isEqualTo(testUser.getRole());
    }

    @Test
    @DisplayName("create — выбрасывает ConflictException при уже существующем email")
    void create_throwsConflictException_whenEmailExists() {
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> teacherService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Пользователь с email " + request.getEmail() + " уже существует");

        verify(roleRepository, never()).findByName(any());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).saveAndFlush(any());
        verify(teacherMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("getById — успешное получение преподавателя")
    void getById_whenTeacherExists_returnsTeacherResponse() {
        // Arrange
        Long teacherId = 1L;
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(testUser));
        when(teacherMapper.toResponse(testUser)).thenReturn(testResponse);

        // Act
        TeacherResponse response = teacherService.getById(teacherId);

        // Assert
        assertThat(response).isEqualTo(testResponse);
        verify(userRepository).findById(teacherId);
        verify(teacherMapper).toResponse(testUser);
    }

    @Test
    @DisplayName("getById — пользователь не найден")
    void getById_whenUserNotFound_throwsResourceNotFoundException() {
        // Arrange
        Long nonExistentId = 999L;
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> teacherService.getById(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Преподаватель с id " + nonExistentId + " не найден");

        verify(userRepository).findById(nonExistentId);
        verify(teacherMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("getById — пользователь найден, но не TEACHER")
    void getById_whenUserIsNotTeacher_throwsResourceNotFoundException() {
        // Arrange
        Long nonTeacherId = 2L;
        Role studentRole = new Role();
        studentRole.setName(RoleName.STUDENT);

        User nonTeacherUser = User.builder()
                .id(nonTeacherId)
                .firstName("Алексей")
                .lastName("Сидоров")
                .email("student@example.com")
                .role(studentRole)
                .build();

        when(userRepository.findById(nonTeacherId)).thenReturn(Optional.of(nonTeacherUser));

        // Act & Assert
        assertThatThrownBy(() -> teacherService.getById(nonTeacherId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Пользователь с id " + nonTeacherId + " не является преподавателем");

        verify(userRepository).findById(nonTeacherId);
        verify(teacherMapper, never()).toResponse(any());
    }

}

