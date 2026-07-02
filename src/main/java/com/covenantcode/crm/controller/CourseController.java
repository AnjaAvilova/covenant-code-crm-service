package com.covenantcode.crm.controller;

import com.covenantcode.crm.dto.course.CourseCreateRequest;
import com.covenantcode.crm.dto.course.CourseResponse;
import com.covenantcode.crm.dto.course.CourseUpdateRequest;
import com.covenantcode.crm.entity.enums.CourseStatus;
import com.covenantcode.crm.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Управление курсами", description = "Эндпоинты для управления курсами")
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Создание нового курса")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Курс успешно создан"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Не авторизован - отсутствует или невалидный JWT токен"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён - у пользователя нет роли ADMIN или MANAGER")
    })
    public CourseResponse create(@Valid @RequestBody CourseCreateRequest request) {
        return courseService.create(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить курс по ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Курс найден"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "404", description = "Курс не найден")
    })
    public ResponseEntity<CourseResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удаление курса по ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Курс удален"),
            @ApiResponse(responseCode = "401", description = "Нет токена или токен недействителен"),
            @ApiResponse(responseCode = "403", description = "Роль не ADMIN"),
            @ApiResponse(responseCode = "404", description = "Курс не найден"),
            @ApiResponse(responseCode = "409", description = "У курса есть активные группы")
    })
    void delete(@PathVariable Long id) {
        courseService.delete(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Обновить курс по ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Курс обновлён"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Нет токена или токен недействителен"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Курс не найден")
    })
    public ResponseEntity<CourseResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CourseUpdateRequest request
    ) {
        return ResponseEntity.ok(courseService.update(id, request));
    }

    @GetMapping
    @Operation(summary = "Получение списка курсов с фильтрацией по статусу и поиску")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список курсов успешно получен"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    public Page<CourseResponse> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CourseStatus status,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        return courseService.getAll(search, status, pageable);
    }
}

