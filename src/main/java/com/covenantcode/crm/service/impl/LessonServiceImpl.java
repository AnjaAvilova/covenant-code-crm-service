package com.covenantcode.crm.service.impl;


import com.covenantcode.crm.dto.lesson.LessonCreateRequest;
import com.covenantcode.crm.dto.lesson.LessonResponse;
import com.covenantcode.crm.dto.lesson.LessonUpdateRequest;
import com.covenantcode.crm.entity.Lesson;
import com.covenantcode.crm.entity.StudyGroup;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.GroupStatus;
import com.covenantcode.crm.exception.BadRequestException;
import com.covenantcode.crm.exception.ForbiddenException;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.mapper.LessonMapper;
import com.covenantcode.crm.repository.LessonRepository;
import com.covenantcode.crm.repository.LessonSpecifications;
import com.covenantcode.crm.repository.StudyGroupRepository;
import com.covenantcode.crm.repository.UserRepository;
import com.covenantcode.crm.service.LessonOverlapService;
import com.covenantcode.crm.service.LessonService;
import com.covenantcode.crm.utils.CurrentUserProvider;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@AllArgsConstructor
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final LessonMapper lessonMapper;
    private final CurrentUserProvider currentUserProvider;
    private final StudyGroupRepository studyGroupRepository;
    private final LessonOverlapService lessonOverlapService;

    @Override
    @Transactional(readOnly = true)
    public LessonResponse getById(Long id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));

        if (currentUserProvider.isTeacher()) {
            checkTeacherHasAccessToLesson(lesson);
        }

        return lessonMapper.toResponse(lesson);
    }

    @Override
    @Transactional
    public LessonResponse create(LessonCreateRequest request) {
        User teacher = userRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getTeacherId()));

        Lesson lesson = lessonMapper.toEntity(request, teacher);

        Lesson savedLesson = lessonRepository.save(lesson);

        return lessonMapper.toResponse(savedLesson);
    }

    @Override
    @Transactional
    public LessonResponse update(Long id, LessonCreateRequest request) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", id));

        User teacher = userRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getTeacherId()));

        lessonMapper.updateEntity(lesson, request, teacher);

        Lesson savedLesson = lessonRepository.save(lesson);

        return lessonMapper.toResponse(savedLesson);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!lessonRepository.existsById(id)) {
            throw new ResourceNotFoundException("Lesson", id);
        }

        lessonRepository.deleteById(id);
    }

    private void checkTeacherHasAccessToLesson(Lesson lesson) {
        Long currentUserId = currentUserProvider.getCurrentUserId();

        if (lesson.getTeacher() == null || !lesson.getTeacher().getId().equals(currentUserId)) {
            throw new ForbiddenException("У вас нет доступа к этому занятию");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LessonResponse> getAll(
            Long groupId,
            Long teacherId,
            LocalDate dateFrom,
            LocalDate dateTo,
            Pageable pageable
    ) {
        Specification<Lesson> spec = Specification.where(null);

        if (currentUserProvider.isTeacher()) {
            Long currentUserId = currentUserProvider.getCurrentUserId();
            spec = spec.and(LessonSpecifications.hasTeacherId(currentUserId));
        } else if (teacherId != null) {
            spec = spec.and(LessonSpecifications.hasTeacherId(teacherId));
        }


        if (groupId != null) {
            spec = spec.and(LessonSpecifications.hasGroupId(groupId));
        }
        if (dateFrom != null) {
            spec = spec.and(LessonSpecifications.hasDateFrom(dateFrom));
        }
        if (dateTo != null) {
            spec = spec.and(LessonSpecifications.hasDateTo(dateTo));
        }


        Page<Lesson> lessonPage = lessonRepository.findAll(spec, pageable);


        return lessonPage.map(lessonMapper::toResponse);
    }
    @Override
    @Transactional
    public LessonResponse update(Long id, LessonUpdateRequest request) {
        // Шаг 1. Загрузка обновляемого занятия
        Lesson existingLesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Lesson с id %d не найдено", id)));

        // Шаг 2. Проверка статуса текущей группы (точный текст с "ё")
        if (existingLesson.getStudyGroup() != null && existingLesson.getStudyGroup().getStatus() == GroupStatus.COMPLETED) {
            throw new BadRequestException("Нельзя редактировать занятия завершённой группы");
        }

        // Шаг 3. Загрузка и проверка новой группы (строго один раз)
        StudyGroup studyGroup = studyGroupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("StudyGroup", request.getGroupId()));

        if (studyGroup.getStatus() != GroupStatus.ACTIVE) {
            throw new BadRequestException("Занятия можно создавать только для активных групп");
        }

        // Шаг 4. Загрузка преподавателя
        User teacher = userRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getTeacherId()));

        // Шаг 5. Проверка корректности временного интервала
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("Время окончания должно быть позже времени начала");
        }

        // Шаг 6. Проверка пересечений (overlap) с исключением текущего занятия (передаем id)
        lessonOverlapService.checkTeacherOverlap(
                request.getTeacherId(),
                request.getLessonDate(),
                request.getStartTime(),
                request.getEndTime(),
                id
        );

        // Обновляем сущность из провалидированных данных (сеттеры исправлены)
        existingLesson.setStudyGroup(studyGroup);
        existingLesson.setTeacher(teacher);
        existingLesson.setTopic(request.getTopic());
        existingLesson.setDescription(request.getDescription());
        existingLesson.setLessonDate(request.getLessonDate());
        existingLesson.setStartTime(request.getStartTime());
        existingLesson.setEndTime(request.getEndTime());

        // Сохраняем обычным save() в переменную
        Lesson savedLesson = lessonRepository.save(existingLesson);

        // Маппер сам заполнит studyGroupId из сущности, ручной сеттер удален
        return lessonMapper.toResponse(savedLesson);
    }

}
