package com.covenantcode.crm.controller;

import com.covenantcode.crm.dto.lesson.LessonResponse;
import com.covenantcode.crm.dto.teacher.TeacherCreateRequest;
import com.covenantcode.crm.dto.teacher.TeacherResponse;
import com.covenantcode.crm.dto.teacher.TeacherUpdateRequest;
import com.covenantcode.crm.dto.user.EnabledUpdateRequest;
import com.covenantcode.crm.service.LessonService;
import com.covenantcode.crm.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
@Tag(name = "teachers", description = "Управление учителями")
public class TeacherController {

    private final TeacherService teacherService;
    private final LessonService lessonService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEACHER')")
    @Operation(summary = "Получить список преподавателей")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список преподавателей"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён")
    })
    public ResponseEntity<Page<TeacherResponse>> getAll(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "lastName") Pageable pageable
    ) {

        return ResponseEntity.ok(teacherService.getAll(search, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Создать нового преподавателя")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Преподаватель создан"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "409", description = "Email уже занят"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён (нужен ADMIN)")
    })
    public ResponseEntity<TeacherResponse> create(@Valid @RequestBody TeacherCreateRequest request) {
        TeacherResponse response = teacherService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Удалить преподавателя")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Преподаватель удалён"),
            @ApiResponse(responseCode = "404", description = "Преподаватель не найден"),
            @ApiResponse(responseCode = "409", description = "Невозможно удалить: есть группы"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён (только ADMIN)")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        teacherService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEACHER')")
    @Operation(summary = "Получить преподавателя по id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Преподаватель найден"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён"),
            @ApiResponse(responseCode = "404", description = "Преподаватель с id не найден")
    })
    public ResponseEntity<TeacherResponse> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(teacherService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Полное обновление профиля преподавателя")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль обновлён"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён (нужен ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Преподаватель не найден")
    })
    public ResponseEntity<TeacherResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TeacherUpdateRequest request) {
        return ResponseEntity.ok(teacherService.update(id, request));
    }

    @PatchMapping("/{id}/enabled")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Блокировка/разблокировка преподавателя")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Статус обновлён"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён (нужен ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Преподаватель не найден")
    })
    public ResponseEntity<TeacherResponse> setEnabled(
            @PathVariable Long id,
            @Valid @RequestBody EnabledUpdateRequest request) {
        return ResponseEntity.ok(teacherService.setEnabled(id, request.getEnabled()));
    }

    @GetMapping("/{teacherId}/lessons")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEACHER')")
    public ResponseEntity<List<LessonResponse>> getLessonsByTeacher(
            @PathVariable Long teacherId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Authentication authentication) {

        // Передаем параметры и объект аутентификации в сервис для проверки ownership check

        List<LessonResponse> lessons = lessonService.getLessonsByTeacher(teacherId, dateFrom, dateTo, authentication);

        return ResponseEntity.ok(lessons);
    }
}
