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
import com.covenantcode.crm.repository.TeacherSpecifications;
import com.covenantcode.crm.repository.UserRepository;
import com.covenantcode.crm.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final UserRepository userRepository;
    private final TeacherMapper teacherMapper;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public Page<TeacherResponse> getAll(String search, Pageable pageable) {
        Specification<User> spec = TeacherSpecifications.hasRole(RoleName.TEACHER);

        if (StringUtils.hasText(search)) {
            spec = spec.and(TeacherSpecifications.searchByText(search));
        }

        return userRepository.findAll(spec, pageable)
                .map(teacherMapper::toResponse);
    }

    @Override
    @Transactional
    public TeacherResponse create(TeacherCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Пользователь с email " + request.getEmail() + " уже существует");
        }

        Role role = roleRepository.findByName(RoleName.TEACHER).orElseThrow(() -> new ResourceNotFoundException("Роль TEACHER не найдена"));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .role(role)
                .build();

        User savedUser = userRepository.saveAndFlush(user);
        return teacherMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Преподаватель с id " + id + " не найден"));

       if(user.getRole().getName() !=RoleName.TEACHER){
           throw new ResourceNotFoundException("Пользователь с id " + id + " не является преподавателем");
        } return teacherMapper.toResponse(user);

    }
}
