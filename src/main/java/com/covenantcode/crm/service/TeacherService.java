package com.covenantcode.crm.service;

import com.covenantcode.crm.dto.teacher.TeacherCreateRequest;
import com.covenantcode.crm.dto.teacher.TeacherResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TeacherService {

    Page<TeacherResponse> getAll(String search, Pageable pageable);

    TeacherResponse create(TeacherCreateRequest request);

    TeacherResponse getById(Long id);
}
