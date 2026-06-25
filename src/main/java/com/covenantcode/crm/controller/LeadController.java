package com.covenantcode.crm.controller;

import com.covenantcode.crm.dto.lead.LeadCommentCreateRequest;
import com.covenantcode.crm.dto.lead.LeadCommentResponse;
import com.covenantcode.crm.dto.lead.LeadConvertRequest;
import com.covenantcode.crm.dto.lead.LeadCreateRequest;
import com.covenantcode.crm.dto.lead.LeadResponse;
import com.covenantcode.crm.dto.student.StudentResponse;
import com.covenantcode.crm.entity.enums.LeadStatus;
import com.covenantcode.crm.service.AuthService;
import com.covenantcode.crm.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;
    private final AuthService authService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Создание нового лида", description = "Создаёт с полями и статусом NEW")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Лид успешно создан",
                    content = @Content(schema = @Schema(implementation = LeadResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса (ошибка валидации)"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав (требуется ADMIN или MANAGER)"),
            @ApiResponse(responseCode = "404", description = "Курс или менеджер не найдены (передан несуществующий ID)")
    })
    public LeadResponse create(@Valid @RequestBody LeadCreateRequest request) {
        return leadService.create(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Получить лида по ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Лид найден"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён"),
            @ApiResponse(responseCode = "404", description = "Лид не найден")
    })
    public LeadResponse getById(@PathVariable @Positive Long id) {
        return leadService.getById(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
            summary = "Получить список лидов с фильтрацией и пагинацией",
            description = "Возвращает страницу лидов с возможностью фильтрации по статусу, менеджеру, курсу и текстовому поиску"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Список лидов найден",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(responseCode = "400", description = "Некорректные параметры запроса"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён")
    })
    public Page<LeadResponse> getAll(
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(required = false) Long assignedManagerId,
            @RequestParam(required = false) Long interestedCourseId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.unsorted());
        return leadService.getAll(search, status, assignedManagerId, interestedCourseId, pageable);
    }

    @PostMapping("/{id}/convert")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Конвертирование лида в студента")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Успешная конвертация лида в студента"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён"),
            @ApiResponse(responseCode = "404", description = "Лид не найден")
    })
    public ResponseEntity<StudentResponse> convertToStudent(
            @PathVariable Long id,
            @Valid @RequestBody LeadConvertRequest request
    ) {
        StudentResponse response = leadService.convertToStudent(id, request);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Добавить комментарий к лиду")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Комментарий успешно добавлен"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён (требуется ADMIN или MANAGER)"),
            @ApiResponse(responseCode = "404", description = "Лид с указанным id не найден")
    })
    public ResponseEntity<LeadCommentResponse> addComment(
            @PathVariable @Positive Long id,
            @Valid @RequestBody LeadCommentCreateRequest request,
            Authentication authentication) {

        Long authorId = authService.getAuthenticatedUserId(authentication);

        LeadCommentResponse response = leadService.addComment(id, request, authorId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}





