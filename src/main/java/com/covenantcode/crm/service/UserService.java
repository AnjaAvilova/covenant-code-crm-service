package com.covenantcode.crm.service;

import com.covenantcode.crm.dto.user.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    Page<UserResponse> getAll(Pageable pageable, String search);

    UserResponse getUserById(Long id, Long currentUserId);

    UserResponse updateEnabled(Long id, boolean enabled, Long currentUserId);
}
