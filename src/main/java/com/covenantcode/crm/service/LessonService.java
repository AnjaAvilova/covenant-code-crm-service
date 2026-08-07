package com.covenantcode.crm.service;

import com.covenantcode.crm.dto.lesson.LessonCreateRequest;
import com.covenantcode.crm.dto.lesson.LessonResponse;
import com.covenantcode.crm.dto.lesson.LessonUpdateRequest;
import java.time.LocalDate;

import java.util.List;
import org.springframework.security.core.Authentication;
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

    List<LessonResponse> getLessonsByTeacher(Long teacherId, LocalDate dateFrom,
                                             LocalDate dateTo, Authentication authentication);

    List<LessonResponse> getLessonsByStudent(Long studentId, LocalDate dateFrom, LocalDate dateTo, Authentication authentication);
}
