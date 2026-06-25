package com.covenantcode.crm.service;

import com.covenantcode.crm.dto.auth.AuthResponse;
import com.covenantcode.crm.dto.auth.LoginRequest;
import com.covenantcode.crm.dto.auth.RegisterRequest;
import com.covenantcode.crm.entity.User;
import org.springframework.security.core.Authentication;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    Long getAuthenticatedUserId(Authentication authentication);
    User getAuthenticatedUser(Authentication authentication);
}
