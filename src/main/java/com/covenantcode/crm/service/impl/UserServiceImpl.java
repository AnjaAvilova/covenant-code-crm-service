package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.user.UserResponse;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.exception.BadRequestException;
import com.covenantcode.crm.exception.ForbiddenException;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.mapper.UserMapper;
import com.covenantcode.crm.repository.UserRepository;
import com.covenantcode.crm.repository.UserSpecifications;
import com.covenantcode.crm.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public Page<UserResponse> getAll(Pageable pageable, String search) {
        if (!StringUtils.hasText(search)) {
            Page<User> userPage = userRepository.findAll(pageable);
            return userPage.map(userMapper::toResponse);
        }

        Specification<User> spec = UserSpecifications.searchByText(search);
        Page<User> userPage = userRepository.findAll(spec, pageable);
        return userPage.map(userMapper::toResponse);
    }

    @Override
    public UserResponse getUserById(Long targetId, Long currentUserId) {

        User targetUser = userRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("User", targetId));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        boolean isOwner = currentUserId.equals(targetId);
        boolean isAdmin = currentUser.getRole() != null
                && currentUser.getRole().getName() == RoleName.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("Доступ запрещён");
        }

        return userMapper.toResponse(targetUser);
    }

    @Override
    @Transactional
    public UserResponse updateEnabled(Long id, boolean enabled, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new BadRequestException("Нельзя заблокировать собственный аккаунт");
        }
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        user.setEnabled(enabled);
        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }
}
