package com.covenantcode.crm.service;

import com.covenantcode.crm.dto.lesson.LessonCreateRequest;
import com.covenantcode.crm.dto.lesson.LessonResponse;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface LessonService {

    LessonResponse getById(Long id);

    LessonResponse create(LessonCreateRequest request);

    LessonResponse update(Long id, LessonCreateRequest request);

    void delete(Long id);

    Page<LessonResponse> getAll(Long groupId, Long TeacherId, LocalDate dateFrom,
                                LocalDate dateTo, Pageable pageable);
}
