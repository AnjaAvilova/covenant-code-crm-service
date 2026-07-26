package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.group.AddStudentToGroupRequest;
import com.covenantcode.crm.dto.group.GroupStatusUpdateRequest;
import com.covenantcode.crm.dto.group.StudyGroupCreateRequest;
import com.covenantcode.crm.dto.group.StudyGroupResponse;
import com.covenantcode.crm.dto.group.StudyGroupUpdateRequest;
import com.covenantcode.crm.dto.lesson.LessonResponse;
import com.covenantcode.crm.dto.student.StudentResponse;
import com.covenantcode.crm.entity.Course;
import com.covenantcode.crm.entity.Student;
import com.covenantcode.crm.entity.StudyGroup;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.GroupStatus;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.exception.BadRequestException;
import com.covenantcode.crm.exception.ConflictException;
import com.covenantcode.crm.exception.ForbiddenException;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.mapper.LessonMapper;
import com.covenantcode.crm.mapper.StudentMapper;
import com.covenantcode.crm.mapper.StudyGroupMapper;
import com.covenantcode.crm.repository.CourseRepository;
import com.covenantcode.crm.repository.LessonRepository;
import com.covenantcode.crm.repository.StudentRepository;
import com.covenantcode.crm.repository.StudyGroupRepository;
import com.covenantcode.crm.repository.UserRepository;
import com.covenantcode.crm.repository.specification.StudyGroupSpecifications;
import com.covenantcode.crm.service.StudyGroupService;
import com.covenantcode.crm.utils.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StudyGroupServiceImpl implements StudyGroupService {

    private final StudyGroupRepository studyGroupRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final StudyGroupMapper studyGroupMapper;
    private final CurrentUserProvider currentUserProvider;

    private final StudentMapper studentMapper;
    private final LessonRepository lessonRepository;
    private final LessonMapper lessonMapper;


    private static final Map<GroupStatus, Set<GroupStatus>> ALLOWED_TRANSITIONS = Map.of(
            GroupStatus.DRAFT, Set.of(GroupStatus.ACTIVE),
            GroupStatus.ACTIVE, Set.of(GroupStatus.COMPLETED, GroupStatus.CANCELLED),
            GroupStatus.COMPLETED, Set.of(),
            GroupStatus.CANCELLED, Set.of()
    );

    @Override
    @Transactional
    public StudyGroupResponse create(StudyGroupCreateRequest request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", request.getCourseId()));

        User teacher = userRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getTeacherId()));

        if (teacher.getRole() == null || !RoleName.TEACHER.equals(teacher.getRole().getName())) {
            throw new BadRequestException("Пользователь с id " + request.getTeacherId() + " не является учителем");
        }

        Set<Student> students = new HashSet<>();
        if (request.getStudentIds() != null && !request.getStudentIds().isEmpty()) {
            for (Long studentId : request.getStudentIds()) {
                Student student = studentRepository.findById(studentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
                students.add(student);
            }
        }

        StudyGroup group = StudyGroup.builder()
                .name(request.getName())
                .course(course)
                .teacher(teacher)
                .startDate(request.getStartDate())
                .students(students)
                .status(GroupStatus.DRAFT)
                .build();

        StudyGroup savedGroup = studyGroupRepository.save(group);

        return studyGroupMapper.toResponse(savedGroup);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StudyGroupResponse> getAll(Long courseId, Long teacherId, GroupStatus status, Pageable pageable) {
        Specification<StudyGroup> specification =
                StudyGroupSpecifications.withFilters(courseId, teacherId, status);

        if (currentUserProvider.isTeacher()) {
            Long currentUserId = currentUserProvider.getCurrentUserId();

            specification = specification.and(
                    StudyGroupSpecifications.hasTeacherId(currentUserId)
            );
        }

        return studyGroupRepository.findAll(specification, pageable)
                .map(studyGroupMapper::toResponse);
    }

    @Override
    @Transactional
    public StudyGroupResponse update(Long id, StudyGroupUpdateRequest request) {
        // 1. Поиск существующей группы
        StudyGroup existingGroup = studyGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StudyGroup с id " + id + " не найдена"));

        // 2. Проверка статуса группы (финальные статусы)
        if (existingGroup.getStatus() == GroupStatus.COMPLETED || existingGroup.getStatus() == GroupStatus.CANCELLED) {
            throw new BadRequestException("Нельзя редактировать группу в статусе " + existingGroup.getStatus());
        }

        // 3. Загрузка и проверка нового курса
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course с id " + request.getCourseId() + " не найден"));

        // 4. Загрузка и проверка нового учителя
        User teacher = userRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("User с id " + request.getTeacherId() + " не найден"));

        // 5. Проверка роли учителя
        if (teacher.getRole() == null || !RoleName.TEACHER.equals(teacher.getRole().getName())) {
            throw new BadRequestException("Пользователь с id " + request.getTeacherId() + " не является учителем");
        }

        // 6. Обновление полей группы
        existingGroup.setName(request.getName());
        existingGroup.setCourse(course);
        existingGroup.setTeacher(teacher);
        existingGroup.setStartDate(request.getStartDate());

        // 7. Сохранение и возврат результата через маппер
        StudyGroup updatedGroup = studyGroupRepository.save(existingGroup);
        return studyGroupMapper.toResponse(updatedGroup);
    }

    @Override
    @Transactional
    public StudyGroupResponse updateStatus(Long id, GroupStatusUpdateRequest request) {

        StudyGroup group = studyGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StudyGroup с id " + id + " не найдена"));

        GroupStatus currentStatus = group.getStatus();
        GroupStatus newStatus = request.getStatus();

        if (!isTransitionAllowed(currentStatus, newStatus)) {
            throw new BadRequestException(
                    String.format("Переход из %s в %s недопустим", currentStatus, newStatus)
            );
        }

        group.setStatus(newStatus);

        StudyGroup savedGroup = studyGroupRepository.save(group);
        return studyGroupMapper.toResponse(savedGroup);
    }

    private boolean isTransitionAllowed(GroupStatus currentStatus, GroupStatus newStatus) {
        Set<GroupStatus> allowedStatuses = ALLOWED_TRANSITIONS.get(currentStatus);
        return allowedStatuses != null && allowedStatuses.contains(newStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentResponse> getGroupStudents(Long id) {
        StudyGroup group = findGroupByIdOrThrow(id);

        if (currentUserProvider.isTeacher()) {
            checkTeacherHasAccessToGroup(group);
        }

        return group.getStudents()
                .stream()
                .map(studentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonResponse> getGroupLessons(Long id) {
        StudyGroup group = findGroupByIdOrThrow(id);

        if (currentUserProvider.isTeacher()) {
            checkTeacherHasAccessToGroup(group);
        }

        return lessonRepository.findAllByStudyGroupId(id)
                .stream()
                .map(lessonMapper::toResponse)
                .toList();
    }

    private StudyGroup findGroupByIdOrThrow(Long id) {
        return studyGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Study group with id " + id + " not found"
                ));
    }

    private void checkTeacherHasAccessToGroup(StudyGroup group) {
        Long currentUserId = currentUserProvider.getCurrentUserId();

        if (group.getTeacher() == null || !group.getTeacher().getId().equals(currentUserId)) {
            throw new ForbiddenException("Нет доступа к этой группе");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StudyGroupResponse getById(Long id, User currentUser) {
        StudyGroup group = studyGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StudyGroup", id));
        RoleName role = currentUser.getRole().getName();

        if (role == RoleName.ADMIN || role == RoleName.MANAGER) {
            return studyGroupMapper.toResponse(group);
        }

        if (role == RoleName.TEACHER) {
            if (group.getTeacher() == null || !group.getTeacher().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Access Denied");
            }
            return studyGroupMapper.toResponse(group);
        }

        if (role == RoleName.STUDENT) {
            Student student = studentRepository.findByUser_Id(currentUser.getId())
                    .orElseThrow(() -> new AccessDeniedException("Access Denied"));
            if (!group.getStudents().contains(student)) {
                throw new AccessDeniedException("Access Denied");
            }
            return studyGroupMapper.toResponse(group);
        }
        throw new AccessDeniedException("Access Denied");
    }

    @Override
    @Transactional
    public void removeStudent(Long groupId, Long studentId) {
        StudyGroup group = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("StudyGroup с id " + groupId + " не найдена"));

        if (group.getStatus().equals(GroupStatus.COMPLETED)) {
            throw new BadRequestException("Нельзя удалить студента из группы в статусе COMPLETED");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student с id " + studentId + " не найден"));

        if (!group.getStudents().contains(student)) {
            throw new BadRequestException(String.format("Студент с id %d не состоит в этой группе", studentId));
        }

        group.getStudents().remove(student);

        studyGroupRepository.save(group);
    }

    @Override
    @Transactional
    public StudyGroupResponse addStudent(Long groupId, AddStudentToGroupRequest request) {
        StudyGroup group = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("StudyGroup с id " + groupId + " не найдена"));

        GroupStatus status = group.getStatus();
        if (status == GroupStatus.COMPLETED || status == GroupStatus.CANCELLED) {
            throw new BadRequestException("Нельзя добавить студента в группу в статусе " + status);
        }

        Long studentId = request.getStudentId();
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student с id " + studentId + " не найден"));

        if (group.getStudents().contains(student)) {
            throw new ConflictException("Студент с id " + studentId + " уже состоит в этой группе");
        }

        group.getStudents().add(student);
        StudyGroup savedGroup = studyGroupRepository.save(group);
        return studyGroupMapper.toResponse(savedGroup);
    }
}