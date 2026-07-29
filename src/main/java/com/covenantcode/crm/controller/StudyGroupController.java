package com.covenantcode.crm.controller;

import com.covenantcode.crm.dto.group.AddStudentToGroupRequest;
import com.covenantcode.crm.dto.group.GroupStatusUpdateRequest;
import com.covenantcode.crm.dto.group.StudyGroupCreateRequest;
import com.covenantcode.crm.dto.group.StudyGroupResponse;
import com.covenantcode.crm.dto.group.StudyGroupUpdateRequest;
import com.covenantcode.crm.dto.lesson.LessonResponse;
import com.covenantcode.crm.dto.student.StudentResponse;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.GroupStatus;
import com.covenantcode.crm.service.StudyGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
@Tag(name = "Учебные группы", description = "API для управления учебными группами")
public class StudyGroupController {

    private final StudyGroupService studyGroupService;

    @Operation(summary = "Создание новой учебной группы", description = "Создаёт группу со статусом DRAFT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Группа успешно создана",
                    content = @Content(schema = @Schema(implementation = StudyGroupResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации или бизнес-логики"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Нет прав доступа"),
            @ApiResponse(responseCode = "404", description = "Ресурс не найден")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StudyGroupResponse> create(@Valid @RequestBody StudyGroupCreateRequest request) {
        StudyGroupResponse response = studyGroupService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Получение списка учебных групп",
            description = "Возвращает постраничный список учебных групп с возможностью фильтрации по курсу, преподавателю и статусу"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список групп успешно получен"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав доступа")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEACHER')")
    public Page<StudyGroupResponse> getAllStudyGroups(
            @Parameter(description = "Идентификатор курса для фильтрации")
            @RequestParam(required = false) Long courseId,
            @Parameter(description = "Идентификатор преподавателя для фильтрации")
            @RequestParam(required = false) Long teacherId,
            @Parameter(description = "Статус группы для фильтрации")
            @RequestParam(required = false) GroupStatus status,
            @ParameterObject
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return studyGroupService.getAll(courseId, teacherId, status, pageable);
    }

    @Operation(
            summary = "Изменение статуса учебной группы",
            description = "Изменяет статус группы с проверкой допустимости перехода по статусной машине"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Статус группы успешно изменён",
                    content = @Content(schema = @Schema(implementation = StudyGroupResponse.class))),
            @ApiResponse(responseCode = "400", description = "Недопустимый переход или ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Нет прав доступа"),
            @ApiResponse(responseCode = "404", description = "Группа не найдена")
    })
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StudyGroupResponse> updateStatus(
            @Parameter(description = "ID группы") @PathVariable Long id,
            @Valid @RequestBody GroupStatusUpdateRequest request) {
        StudyGroupResponse response = studyGroupService.updateStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
            summary = "Обновить учебную группу",
            description = "Обновляет данные существующей учебной группы по её идентификатору. Доступно для ролей ADMIN и MANAGER."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Группа успешно обновлена",
                    content = @Content(schema = @Schema(implementation = StudyGroupResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные входные данные или попытка изменить группу в финальном статусе",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не аутентифицирован",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав (требуется роль ADMIN или MANAGER)",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Учебная группа, курс или учитель не найдены",
                    content = @Content
            )
    })
    public ResponseEntity<StudyGroupResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody StudyGroupUpdateRequest request
    ) {
        StudyGroupResponse response = studyGroupService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEACHER')")
    @Operation(summary = "Получение списка студентов учебной группы",
            description = "Возвращает полный список студентов, зачисленных в группу. " +
                    "Доступ: ADMIN/MANAGER – любая группа, TEACHER – только те группы, где он является преподавателем.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение списка студентов"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Нет прав на просмотр студентов этой группы"),
            @ApiResponse(responseCode = "404", description = "Группа не найдена"),

    })
    public ResponseEntity<List<StudentResponse>> getStudentsOfGroup(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(studyGroupService.getStudentsOfGroup(id, currentUser));
    }

    @GetMapping("/{id}/lessons")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEACHER')")
    public ResponseEntity<List<LessonResponse>> getGroupLessons(@PathVariable Long id) {
        return ResponseEntity.ok(studyGroupService.getGroupLessons(id));
    }

    @Operation(
            summary = "Получение учебной группы по ID",
            description = "Возвращает подробную информацию о группе. Доступ: ADMIN/MANAGER – любая группа, TEACHER – только свои, STUDENT – только свои."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Группа найдена",
                    content = @Content(schema = @Schema(implementation = StudyGroupResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Нет прав на просмотр этой группы"),
            @ApiResponse(responseCode = "404", description = "Группа не найдена")
    })
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StudyGroupResponse> getGroupById(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(studyGroupService.getById(id, currentUser));
    }

    @Operation(
            summary = "Удаление студента из учебной группы"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Студент успешно удалён"),
            @ApiResponse(responseCode = "400", description = "Недопустимый переход или ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Нет прав доступа"),
            @ApiResponse(responseCode = "404", description = "Группа не найдена")
    })
    @DeleteMapping("/{id}/students/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeStudent(@PathVariable("id") Long groupId,
                              @PathVariable Long studentId) {
        studyGroupService.removeStudent(groupId, studentId);
    }

    @Operation(
            summary = "Добавить студента в группу",
            description = "Добавляет существующего студента в учебную группу. Доступно для ADMIN и MANAGER."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Студент добавлен, возвращена обновлённая группа",
                    content = @Content(schema = @Schema(implementation = StudyGroupResponse.class))),
            @ApiResponse(responseCode = "400", description = "Группа в финальном статусе или ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Нет прав доступа"),
            @ApiResponse(responseCode = "404", description = "Группа или студент не найдены"),
            @ApiResponse(responseCode = "409", description = "Студент уже состоит в группе")
    })
    @PostMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StudyGroupResponse> addStudent(
            @Parameter(description = "ID группы") @PathVariable Long id,
            @Valid @RequestBody AddStudentToGroupRequest request) {
        StudyGroupResponse response = studyGroupService.addStudent(id, request);
        return ResponseEntity.ok(response);
    }
}
