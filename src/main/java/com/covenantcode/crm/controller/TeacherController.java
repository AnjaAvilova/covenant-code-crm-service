package com.covenantcode.crm.controller;

import com.covenantcode.crm.dto.teacher.TeacherCreateRequest;
import com.covenantcode.crm.dto.teacher.TeacherResponse;
import com.covenantcode.crm.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
@Tag(name = "teachers", description = "Управление учителями")
public class TeacherController {

    private final TeacherService teacherService;

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
            @PageableDefault(size = 20, sort = "lastName") Pageable pageable) {
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

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEACHER')")
    @Operation(summary = "Получить преподавателя по id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Преподаватель найден"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён"),
            @ApiResponse(responseCode = "404", description = "Преподаватель с id {id} не найден")
    })
    public ResponseEntity<TeacherResponse> getById(@PathVariable Long id){
                return ResponseEntity.ok(teacherService.getById(id));
    }
}
