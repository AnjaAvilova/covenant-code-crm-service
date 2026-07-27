package com.covenantcode.crm.service.integration;

import com.covenantcode.crm.BaseIntegrationTest;
import com.covenantcode.crm.entity.Course;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.StudyGroup;
import com.covenantcode.crm.entity.Role;
import com.covenantcode.crm.entity.Lesson;
import com.covenantcode.crm.entity.enums.CourseStatus;
import com.covenantcode.crm.entity.enums.GroupStatus;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.exception.ConflictException;
import com.covenantcode.crm.repository.StudyGroupRepository;
import com.covenantcode.crm.repository.UserRepository;
import com.covenantcode.crm.repository.CourseRepository;
import com.covenantcode.crm.repository.RoleRepository;
import com.covenantcode.crm.repository.LessonRepository;
import com.covenantcode.crm.service.LessonOverlapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class LessonOverlapServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private LessonOverlapService lessonOverlapService;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StudyGroupRepository studyGroupRepository;

    @Autowired
    private CourseRepository courseRepository;

    private static final LocalDate LESSON_DATE = LocalDate.of(2026, 5, 20);
    private User teacher;
    private StudyGroup studyGroup;
    private Course course;

    @BeforeEach
    void setUp() {

        lessonRepository.deleteAll();
        studyGroupRepository.deleteAll();
        userRepository.deleteAll();
        courseRepository.deleteAll();

        createRoleIfNotExists(RoleName.ADMIN);
        createRoleIfNotExists(RoleName.MANAGER);
        createRoleIfNotExists(RoleName.TEACHER);
        createRoleIfNotExists(RoleName.STUDENT);

        Role teacherRole = roleRepository.findByName(RoleName.TEACHER)
                .orElseThrow(() -> new RuntimeException("Role TEACHER not found in database"));

        teacher = User.builder()
                .firstName("Test")
                .lastName("Teacher")
                .email("teacher@test.com")
                .password("password")
                .role(teacherRole)
                .enabled(true)
                .build();
        teacher = userRepository.save(teacher);

        User groupTeacher = User.builder()
                .firstName("Group")
                .lastName("Teacher")
                .email("groupteacher@test.com")
                .password("password")
                .role(teacherRole)
                .enabled(true)
                .build();
        groupTeacher = userRepository.save(groupTeacher);

        course = Course.builder()
                .title("Test Course")
                .description("Test Course Description")
                .durationInWeeks(8)
                .price(new BigDecimal("1000.00"))
                .status(CourseStatus.ACTIVE)
                .build();
        course = courseRepository.save(course);

        studyGroup = StudyGroup.builder()
                .name("Test Group")
                .course(course)
                .teacher(groupTeacher)
                .startDate(LocalDate.of(2026, 5, 1))  // ОБЯЗАТЕЛЬНО! Добавляем дату начала
                .status(GroupStatus.ACTIVE)
                .build();
        studyGroup = studyGroupRepository.save(studyGroup);
    }

    private void createRoleIfNotExists(RoleName roleName) {
        if (!roleRepository.findByName(roleName).isPresent()) {
            Role role = Role.builder()
                    .name(roleName)
                    .build();
            roleRepository.save(role);
        }
    }

    @Test
    void shouldNotThrowExceptionWhenNoConflict() {
        Lesson existingLesson = Lesson.builder()
                .teacher(teacher)
                .studyGroup(studyGroup)
                .lessonDate(LESSON_DATE)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .topic("Test Lesson")
                .description("Test description")
                .build();
        lessonRepository.save(existingLesson);

        assertThatCode(() -> lessonOverlapService.checkTeacherOverlap(
                teacher.getId(),
                LESSON_DATE,
                LocalTime.of(10, 30),
                LocalTime.of(12, 0),
                null
        )).doesNotThrowAnyException();
    }

    @Test
    void shouldThrowConflictExceptionWhenConflictExists() {
        Lesson existingLesson = Lesson.builder()
                .teacher(teacher)
                .studyGroup(studyGroup)
                .lessonDate(LESSON_DATE)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .topic("Test Lesson")
                .description("Test description")
                .build();
        lessonRepository.save(existingLesson);

        assertThatThrownBy(() -> lessonOverlapService.checkTeacherOverlap(
                teacher.getId(),
                LESSON_DATE,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                null
        )).isInstanceOf(ConflictException.class)
                .hasMessageContaining("Преподаватель уже занят в это время");
    }

    @Test
    void shouldNotThrowExceptionWhenLessonEndsExactlyWhenAnotherStarts() {
        Lesson existingLesson = Lesson.builder()
                .teacher(teacher)
                .studyGroup(studyGroup)
                .lessonDate(LESSON_DATE)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .topic("Test Lesson")
                .description("Test description")
                .build();
        lessonRepository.save(existingLesson);

        assertThatCode(() -> lessonOverlapService.checkTeacherOverlap(
                teacher.getId(),
                LESSON_DATE,
                LocalTime.of(11, 0),
                LocalTime.of(13, 0),
                null
        )).doesNotThrowAnyException();
    }

    @Test
    void shouldThrowConflictExceptionWhenMultipleLessonsExist() {
        Lesson lesson1 = Lesson.builder()
                .teacher(teacher)
                .studyGroup(studyGroup)
                .lessonDate(LESSON_DATE)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .topic("Lesson 1")
                .description("First lesson")
                .build();
        lessonRepository.save(lesson1);

        Lesson lesson2 = Lesson.builder()
                .teacher(teacher)
                .studyGroup(studyGroup)
                .lessonDate(LESSON_DATE)
                .startTime(LocalTime.of(11, 0))
                .endTime(LocalTime.of(12, 0))
                .topic("Lesson 2")
                .description("Second lesson")
                .build();
        lessonRepository.save(lesson2);

        assertThatThrownBy(() -> lessonOverlapService.checkTeacherOverlap(
                teacher.getId(),
                LESSON_DATE,
                LocalTime.of(9, 30),
                LocalTime.of(11, 30),
                null
        )).isInstanceOf(ConflictException.class)
                .hasMessageContaining("Преподаватель уже занят в это время");
    }

    @Test
    void shouldNotThrowExceptionWhenExcludingOwnLesson() {
        Lesson existingLesson = Lesson.builder()
                .teacher(teacher)
                .studyGroup(studyGroup)
                .lessonDate(LESSON_DATE)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .topic("Test Lesson")
                .description("Test description")
                .build();
        Lesson savedLesson = lessonRepository.save(existingLesson);

        assertThatCode(() -> lessonOverlapService.checkTeacherOverlap(
                teacher.getId(),
                LESSON_DATE,
                LocalTime.of(9, 0),
                LocalTime.of(11, 0),
                savedLesson.getId()
        )).doesNotThrowAnyException();
    }

    @Test
    void shouldNotThrowExceptionWhenDifferentDay() {
        Lesson existingLesson = Lesson.builder()
                .teacher(teacher)
                .studyGroup(studyGroup)
                .lessonDate(LocalDate.of(2026, 5, 20))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .topic("Test Lesson")
                .description("Test description")
                .build();
        lessonRepository.save(existingLesson);

        assertThatCode(() -> lessonOverlapService.checkTeacherOverlap(
                teacher.getId(),
                LocalDate.of(2026, 5, 21),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                null
        )).doesNotThrowAnyException();
    }

    @Test
    void shouldNotThrowExceptionWhenDifferentTeacher() {
        Role teacherRole = roleRepository.findByName(RoleName.TEACHER)
                .orElseThrow(() -> new RuntimeException("Role TEACHER not found"));

        User teacher1 = User.builder()
                .firstName("Teacher1")
                .lastName("Test")
                .email("teacher1@test.com")
                .password("password")
                .role(teacherRole)
                .enabled(true)
                .build();
        teacher1 = userRepository.save(teacher1);

        User teacher2 = User.builder()
                .firstName("Teacher2")
                .lastName("Test")
                .email("teacher2@test.com")
                .password("password")
                .role(teacherRole)
                .enabled(true)
                .build();
        teacher2 = userRepository.save(teacher2);

        StudyGroup groupForTest = StudyGroup.builder()
                .name("Test Group 2")
                .course(course)
                .teacher(teacher1)
                .startDate(LocalDate.of(2026, 5, 1))  // ОБЯЗАТЕЛЬНО!
                .status(GroupStatus.ACTIVE)
                .build();
        groupForTest = studyGroupRepository.save(groupForTest);

        Lesson existingLesson = Lesson.builder()
                .teacher(teacher1)
                .studyGroup(groupForTest)
                .lessonDate(LESSON_DATE)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .topic("Test Lesson")
                .description("Test description")
                .build();
        lessonRepository.save(existingLesson);

        Long teacher2Id = teacher2.getId();

        assertThatCode(() -> lessonOverlapService.checkTeacherOverlap(
                teacher2Id,
                LESSON_DATE,
                LocalTime.of(9, 0),
                LocalTime.of(11, 0),
                null
        )).doesNotThrowAnyException();
    }
}