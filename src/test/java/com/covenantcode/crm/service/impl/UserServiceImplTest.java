package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.user.UserResponse;
import com.covenantcode.crm.entity.Role;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.exception.BadRequestException;
import com.covenantcode.crm.exception.ForbiddenException;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.mapper.UserMapper;
import com.covenantcode.crm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private Pageable pageable;
    private OffsetDateTime now;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 20);
        now = OffsetDateTime.now();
    }

    @Test
    public void getAll_success() {
        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName(RoleName.ADMIN);

        Role managerRole = new Role();
        managerRole.setId(2L);
        managerRole.setName(RoleName.MANAGER);

        User user1 = User.builder()
                .id(1L)
                .firstName("Admin")
                .lastName("System")
                .email("admin@covenantcode.ru")
                .phone(null)
                .role(adminRole)
                .enabled(true)
                .createdAt(now)
                .build();

        User user2 = User.builder()
                .id(2L)
                .firstName("Иван")
                .lastName("Петров")
                .email("ivan.petrov@company.ru")
                .phone("+79161234567")
                .role(managerRole)
                .enabled(true)
                .createdAt(now)
                .build();

        List<User> users = List.of(user1, user2);
        Page<User> userPage = new PageImpl<>(users, pageable, users.size());

        UserResponse response1 = UserResponse.builder()
                .id(1L)
                .firstName("Admin")
                .lastName("System")
                .email("admin@covenantcode.ru")
                .phone(null)
                .role("ADMIN")
                .enabled(true)
                .createdAt(now.toString())
                .build();

        UserResponse response2 = UserResponse.builder()
                .id(2L)
                .firstName("Иван")
                .lastName("Петров")
                .email("ivan.petrov@company.ru")
                .phone("+79161234567")
                .role("MANAGER")
                .enabled(true)
                .createdAt(now.toString())
                .build();

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toResponse(user1)).thenReturn(response1);
        when(userMapper.toResponse(user2)).thenReturn(response2);

        Page<UserResponse> result = userService.getAll(pageable, null);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());

        UserResponse firstResponse = result.getContent().get(0);
        assertEquals("ADMIN", firstResponse.getRole());

        UserResponse secondResponse = result.getContent().get(1);
        assertEquals("MANAGER", secondResponse.getRole());

        verify(userRepository).findAll(pageable);
        verify(userMapper).toResponse(user1);
        verify(userMapper).toResponse(user2);
    }

    @Test
    public void getAll_emptyList() {
        Page<User> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(userRepository.findAll(pageable)).thenReturn(emptyPage);

        Page<UserResponse> result = userService.getAll(pageable, null);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());

        verify(userRepository).findAll(pageable);
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void getUserById_whenAdminRequestsAnotherUser_thenSuccess() {

        Long adminId = 1L;
        Long targetId = 2L;
        User adminUser = buildUser(adminId, RoleName.ADMIN);
        User targetUser = buildUser(targetId, RoleName.MANAGER);
        UserResponse expectedResponse = new UserResponse();

        when(userRepository.findById(targetId)).thenReturn(Optional.of(targetUser));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(userMapper.toResponse(targetUser)).thenReturn(expectedResponse);

        UserResponse result = userService.getUserById(targetId, adminId);

        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(userRepository).findById(targetId);
        verify(userRepository).findById(adminId);
        verify(userMapper).toResponse(targetUser);
    }

    @Test
    void getUserById_whenUserRequestsOwnProfile_thenSuccess() {

        Long userId = 5L;
        User user = buildUser(userId, RoleName.MANAGER);
        UserResponse expectedResponse = new UserResponse();


        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expectedResponse);

        UserResponse result = userService.getUserById(userId, userId);

        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(userRepository, times(2)).findById(userId);
    }

    @Test
    void getUserById_whenManagerRequestsAnotherUser_thenForbiddenException() {

        Long managerId = 5L;
        Long otherUserId = 7L;

        User managerUser = buildUser(managerId, RoleName.MANAGER);
        User otherUser = buildUser(otherUserId, RoleName.MANAGER);

        when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));
        when(userRepository.findById(managerId)).thenReturn(Optional.of(managerUser));

        assertThrows(ForbiddenException.class, () ->
                userService.getUserById(otherUserId, managerId)
        );

        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void getUserById_whenTargetUserNotFound_thenEntityNotFoundException() {

        Long targetId = 1L;
        Long currentUserId = 1L;
        when(userRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserById(targetId, currentUserId));

        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void getUserById_whenCurrentUserNotFound_thenEntityNotFoundException() {

        Long targetId = 1L;
        Long currentUserId = 2L;

        User targetUser = new User();
        targetUser.setId(targetId);

        when(userRepository.findById(targetId)).thenReturn(Optional.of(targetUser));

        when(userRepository.findById(currentUserId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserById(targetId, currentUserId);
        });

        verify(userRepository).findById(targetId);
        verify(userRepository).findById(currentUserId);
    }

    private User buildUser(Long id, RoleName roleName) {
        Role role = new Role();
        role.setName(roleName);

        User user = new User();
        user.setId(id);
        user.setEmail("user_" + id + "@test.com");
        user.setFirstName("User");
        user.setLastName(String.valueOf(id));
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }

    @Test
    public void updateEnabled_whenAdminBlocksAnotherUser(){
        Long adminId = 1L;
        Long targetUserId = 2L;
        boolean enabledStatus = false;

        User targetUser = new User();
        targetUser.setId(targetUserId);
        targetUser.setEnabled(true);

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User userToSave = invocation.getArgument(0);
            userToSave.setEnabled(enabledStatus);
            return userToSave;
        });
        assertThatCode(() -> userService.updateEnabled(targetUserId, enabledStatus, adminId))
                .doesNotThrowAnyException();
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    public void updateEnabled_whenUserBlocksHimself(){
        Long adminId = 1L;
        boolean enabledStatus = false;

        assertThatThrownBy(() -> userService.updateEnabled(adminId, enabledStatus, adminId))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Нельзя заблокировать собственный аккаунт");
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    public void updateEnabled_whenUserNotFound(){
        Long nonExistentUserId = 999L;
        boolean enabledStatus = false;
        Long currentUserId = 1L;

        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.updateEnabled(nonExistentUserId, enabledStatus, currentUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Пользователь не найден");
        verify(userRepository, never()).save(any());
    }

    @Test
    public void updateEnabled_whenUserAlreadyActive(){
        Long adminId = 1L;
        Long targetUserId = 2L;
        boolean enabledStatus = true;

        User targetUser = new User();
        targetUser.setId(targetUserId);
        targetUser.setEnabled(true);

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User userToSave = invocation.getArgument(0);
            userToSave.setEnabled(enabledStatus);
            return userToSave;
        });
        assertThatCode(() -> userService.updateEnabled(targetUserId, enabledStatus, adminId))
                .doesNotThrowAnyException();
        assertThatCode(() -> userService.updateEnabled(targetUserId, enabledStatus, adminId))
                .doesNotThrowAnyException();

        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    public void getAll_whenSearchIsNull_thenFindAllWithoutSpecification() {
        Page<User> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(userRepository.findAll(pageable)).thenReturn(emptyPage);

        Page<UserResponse> result = userService.getAll(pageable, null);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());

        verify(userRepository).findAll(pageable);
        verify(userRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getAll_whenSearchIsValid_shouldCallFindAllWithSpecificationAndReturnFilteredResult() {
        String search = "иван";

        Role managerRole = new Role();
        managerRole.setId(2L);
        managerRole.setName(RoleName.MANAGER);

        User user = User.builder()
                .id(2L)
                .firstName("Иван")
                .lastName("Петров")
                .email("ivan.petrov@company.ru")
                .phone("+79161234567")
                .role(managerRole)
                .enabled(true)
                .createdAt(now)
                .build();

        UserResponse response = UserResponse.builder()
                .id(2L)
                .firstName("Иван")
                .lastName("Петров")
                .email("ivan.petrov@company.ru")
                .phone("+79161234567")
                .role("MANAGER")
                .enabled(true)
                .createdAt(now.toString())
                .build();

        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);

        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);
        when(userMapper.toResponse(user)).thenReturn(response);

        Page<UserResponse> result = userService.getAll(pageable, search);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());

        UserResponse firstResponse = result.getContent().get(0);
        assertEquals("Иван", firstResponse.getFirstName());
        assertEquals("Петров", firstResponse.getLastName());
        assertEquals("ivan.petrov@company.ru", firstResponse.getEmail());

        verify(userRepository, times(1)).findAll(any(Specification.class), eq(pageable));
        verify(userRepository, never()).findAll(pageable);
        verify(userMapper, times(1)).toResponse(user);
    }

    @Test
    void getAll_whenSearchIsEmptyString_shouldTreatAsNullAndCallFindAllWithoutSpecification() {
        String search = "";

        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName(RoleName.ADMIN);

        User user = User.builder()
                .id(1L)
                .firstName("Admin")
                .lastName("System")
                .email("admin@covenantcode.ru")
                .role(adminRole)
                .enabled(true)
                .createdAt(now)
                .build();

        UserResponse response = UserResponse.builder()
                .id(1L)
                .firstName("Admin")
                .lastName("System")
                .email("admin@covenantcode.ru")
                .role("ADMIN")
                .enabled(true)
                .createdAt(now.toString())
                .build();

        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toResponse(user)).thenReturn(response);

        Page<UserResponse> result = userService.getAll(pageable, search);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("admin@covenantcode.ru");

        verify(userRepository, times(1)).findAll(pageable);
        verify(userRepository, never()).findAll(any(Specification.class), any(Pageable.class));
        verify(userMapper, times(1)).toResponse(user);
    }
}
