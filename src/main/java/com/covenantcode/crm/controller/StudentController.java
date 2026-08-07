package com.covenantcode.crm.controller;

import com.covenantcode.crm.dto.lesson.LessonResponse;
import com.covenantcode.crm.dto.student.StudentCreateRequest;
import com.covenantcode.crm.dto.student.StudentResponse;
import com.covenantcode.crm.dto.student.StudentUpdateRequest;
import com.covenantcode.crm.service.LessonService;
import com.covenantcode.crm.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Tag(name = "Students", description = "Управление студентами")
public class StudentController {

    private final StudentService studentService;
    private final LessonService lessonService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Создать нового студента", description = "Доступно ролям ADMIN, MANAGER")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Студент успешно создан"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации входных данных"),
            @ApiResponse(responseCode = "404", description = "Указанный userId не найден"),
            @ApiResponse(responseCode = "409", description = "Пользователь уже привязан к другому студенту")
    })
    public StudentResponse create(@Valid @RequestBody StudentCreateRequest request) {
        return studentService.create(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEACHER')")
    @Operation(
            summary = "Получить список студентов с пагинацией и поиском",
            description = "ADMIN и MANAGER видят всех студентов, TEACHER видит только студентов своих групп"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Список студентов найден",
                    content =
                    @Content(mediaType = "application/json")
            ),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён")
    })
    public ResponseEntity<Page<StudentResponse>> getAll(
            @RequestParam(required = false) String search,
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(studentService.getAll(search, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Обновить данные студента", description = "Обновляет все поля существующего студента (кроме привязки к учётной записи)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Данные успешно обновлены"),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос (ошибки валидации)"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён (недостаточно прав)"),
            @ApiResponse(responseCode = "404", description = "Студент с указанным ID не найден")
    })
    public ResponseEntity<StudentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody StudentUpdateRequest request) {
        StudentResponse response = studentService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEACHER', 'STUDENT')")
    @Operation(summary = "Получить студента по ID",
            description = "ADMIN и MANAGER могут получить любого студента, TEACHER — только студента из своих групп"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Студент найден"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён"),
            @ApiResponse(responseCode = "404", description = "Студент не найден")
    })
    public ResponseEntity<StudentResponse> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(studentService.getById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Удалить студента", description = "Доступно только роли ADMIN. Студент не может быть удалён, если состоит в активной группе")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Студент успешно удалён"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён (требуется роль ADMIN)"),
            @ApiResponse(responseCode = "404", description = "Студент с указанным ID не найден"),
            @ApiResponse(responseCode = "409", description = "Студент состоит в активной группе и не может быть удалён")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studentService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{studentId}/lessons")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STUDENT')")
    public ResponseEntity<List<LessonResponse>> getLessonsByStudent(
            @PathVariable Long studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Authentication authentication) {
        List<LessonResponse> lesson = lessonService.getLessonsByStudent(studentId, dateFrom, dateTo, authentication);
        return ResponseEntity.ok(lesson);
    }
}
