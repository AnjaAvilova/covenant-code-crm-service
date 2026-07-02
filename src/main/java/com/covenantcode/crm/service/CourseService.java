package com.covenantcode.crm.service;

import com.covenantcode.crm.dto.course.CourseCreateRequest;
import com.covenantcode.crm.dto.course.CourseResponse;
import com.covenantcode.crm.dto.course.CourseUpdateRequest;
import com.covenantcode.crm.entity.enums.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseService {
    CourseResponse create(CourseCreateRequest request);
    CourseResponse getById(Long id);
    void delete(Long id);
    CourseResponse update(Long id, CourseUpdateRequest request);
    Page<CourseResponse> getAll(String search, CourseStatus status, Pageable pageable);
}
