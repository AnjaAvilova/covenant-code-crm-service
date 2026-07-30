package com.covenantcode.crm.service;

import com.covenantcode.crm.dto.lesson.LessonCreateRequest;
import com.covenantcode.crm.dto.lesson.LessonResponse;
import com.covenantcode.crm.dto.lesson.LessonUpdateRequest;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;


public interface LessonService {

    LessonResponse getById(Long id, Authentication authentication);

    LessonResponse create(LessonCreateRequest request);

    void delete(Long id);

    Page<LessonResponse> getAll(Long groupId, Long TeacherId, LocalDate dateFrom,
                                LocalDate dateTo, Pageable pageable);

    LessonResponse update(Long id, LessonUpdateRequest request);
}
