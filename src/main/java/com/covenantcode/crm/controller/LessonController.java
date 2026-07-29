package com.covenantcode.crm.controller;

import com.covenantcode.crm.dto.lesson.LessonCreateRequest;
import com.covenantcode.crm.dto.lesson.LessonResponse;
import com.covenantcode.crm.dto.lesson.LessonUpdateRequest;
import com.covenantcode.crm.service.LessonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/lessons")
public class LessonController {

    private final LessonService lessonService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public LessonResponse create(@Valid @RequestBody LessonCreateRequest request) {
        return lessonService.create(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEACHER')")
    public ResponseEntity<LessonResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(lessonService.getById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        lessonService.delete(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEACHER')")
    @Operation(
            summary = "Получить список занятий",
            description = "Возвращает пагинированный список занятий. Администраторы и менеджеры видят все занятия, преподаватели — только свои."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Список занятий успешно получен",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))
            ),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен (недостаточно прав)", content = @Content)
    })
    public ResponseEntity<Page<LessonResponse>> getAll(
            @Parameter(description = "Идентификатор учебной группы")
            @RequestParam(required = false) Long groupId,

            @Parameter(description = "Идентификатор преподавателя")
            @RequestParam(required = false) Long teacherId,

            @Parameter(description = "Нижняя граница даты проведения (включительно), формат YYYY-MM-DD")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,

            @Parameter(description = "Верхняя граница даты проведения (включительно), формат YYYY-MM-DD")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,

            @Parameter(description = "Параметры пагинации и сортировки (page, size, sort)")
            Pageable pageable
    ) {
        Page<LessonResponse> response = lessonService.getAll(groupId, teacherId, dateFrom, dateTo, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
            summary = "Обновить занятие",
            description = "Полное обновление атрибутов занятия")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешное обновление занятия",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LessonResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Неверный запрос", content = @Content),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован", content = @Content),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен (недостаточно прав)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Занятие не найдено", content = @Content),
            @ApiResponse(responseCode = "409", description = "У преподавателя уже есть занятие в это время", content = @Content)
    })
    public ResponseEntity<LessonResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody LessonUpdateRequest request
    ) {
        LessonResponse response = lessonService.update(id, request);
        return ResponseEntity.ok(response);
    }
}
