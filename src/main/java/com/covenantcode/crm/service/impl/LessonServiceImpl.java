package com.covenantcode.crm.service.impl;


import com.covenantcode.crm.dto.lesson.LessonCreateRequest;
import com.covenantcode.crm.dto.lesson.LessonResponse;
import com.covenantcode.crm.dto.lesson.LessonUpdateRequest;
import com.covenantcode.crm.entity.Lesson;
import com.covenantcode.crm.entity.Role;
import com.covenantcode.crm.entity.Student;
import com.covenantcode.crm.entity.StudyGroup;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.GroupStatus;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.exception.BadRequestException;
import com.covenantcode.crm.exception.ForbiddenException;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.mapper.LessonMapper;
import com.covenantcode.crm.repository.LessonRepository;
import com.covenantcode.crm.repository.LessonSpecifications;
import com.covenantcode.crm.repository.StudentRepository;
import com.covenantcode.crm.repository.StudyGroupRepository;
import com.covenantcode.crm.repository.UserRepository;
import com.covenantcode.crm.service.LessonOverlapService;
import com.covenantcode.crm.service.LessonService;
import com.covenantcode.crm.utils.CurrentUserProvider;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final StudentRepository studentRepository;

    @Override
    @Transactional(readOnly = true)
    public LessonResponse getById(Long id, Authentication authentication) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", id));

        User currentUser = extractUserFromAuthentication(authentication);
        checkAccess(lesson, currentUser);

        return lessonMapper.toResponse(lesson);
    }

    @Override
    @Transactional
    public LessonResponse create(LessonCreateRequest request) {
        validateLessonTime(request.getStartTime(), request.getEndTime());

        StudyGroup studyGroup = studyGroupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("StudyGroup", request.getGroupId()));

        validateStudyGroupIsActive(studyGroup);

        User teacher = userRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getTeacherId()));

        lessonOverlapService.checkTeacherOverlap(
                teacher.getId(),
                request.getLessonDate(),
                request.getStartTime(),
                request.getEndTime(),
                null
        );

        Lesson lesson = lessonMapper.toEntity(request);
        lesson.setStudyGroup(studyGroup);
        lesson.setTeacher(teacher);

        Lesson savedLesson = lessonRepository.save(lesson);

        return lessonMapper.toResponse(savedLesson);
    }

    @Transactional
    @Override
    public LessonResponse update(Long id, LessonUpdateRequest request) {
        validateLessonTime(request.getStartTime(), request.getEndTime());

        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", id));


        StudyGroup studyGroup = studyGroupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("StudyGroup", request.getGroupId()));

        validateStudyGroupIsActive(studyGroup);

        User teacher = userRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getTeacherId()));

        lessonOverlapService.checkTeacherOverlap(
                teacher.getId(),
                request.getLessonDate(),
                request.getStartTime(),
                request.getEndTime(),
                id
        );

        lessonMapper.updateEntity(lesson, request);
        lesson.setStudyGroup(studyGroup);
        lesson.setTeacher(teacher);

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

    private void validateLessonTime(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            throw new BadRequestException("Обязательное время начала и окончания урок");
        }

        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException("Время окончания урока должно быть позже времени начала");
        }
    }

    private void validateStudyGroupIsActive(StudyGroup studyGroup) {
        if (studyGroup.getStatus() != GroupStatus.ACTIVE) {
            throw new BadRequestException("Группа студентов должна быть в статусе ACTIVE");
        }
    }


    private User extractUserFromAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        if (principal instanceof UserDetails userDetails) {
            String email = userDetails.getUsername();
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new ForbiddenException("Пользователь не найден"));
        }
        throw new ForbiddenException("Не удалось определить пользователя");
    }

    private void checkAccess(Lesson lesson, User currentUser) {
        Role userRole = currentUser.getRole();
        if (userRole == null) {
            throw new ForbiddenException("У пользователя не назначена роль");
        }

        RoleName roleName = userRole.getName();

        if (roleName == RoleName.ADMIN || roleName == RoleName.MANAGER) {
            return;
        }

        if (roleName == RoleName.TEACHER) {
            if (lesson.getTeacher() == null) {
                throw new ForbiddenException("У занятия не указан преподаватель");
            }
            if (!lesson.getTeacher().getId().equals(currentUser.getId())) {
                throw new ForbiddenException("У вас нет доступа к этому занятию");
            }
            return;
        }

        if (roleName == RoleName.STUDENT) {
            Student student = studentRepository.findByUser_Id(currentUser.getId())
                    .orElseThrow(() -> new ForbiddenException("Студент не найден"));

            if (lesson.getStudyGroup() == null) {
                throw new ForbiddenException("Занятие не привязано к группе");
            }

            if (!lesson.getStudyGroup().getStudents().contains(student)) {
                throw new ForbiddenException("У вас нет доступа к этому занятию");
            }
            return;
        }

        throw new ForbiddenException("Недостаточно прав");
    }
}
