package com.covenantcode.crm.repository;

import com.covenantcode.crm.entity.Lesson;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public class LessonSpecifications {
    private LessonSpecifications() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Фильтр по идентификатору учебной группы (studyGroup.id).
     */
    public static Specification<Lesson> hasGroupId(Long groupId) {
        return (root, query, cb) -> groupId == null
                ? null
                : cb.equal(root.get("studyGroup").get("id"), groupId);
    }

    /**
     * Фильтр по идентификатору преподавателя (teacher.id).
     */
    public static Specification<Lesson> hasTeacherId(Long teacherId) {
        return (root, query, cb) -> teacherId == null
                ? null
                : cb.equal(root.get("teacher").get("id"), teacherId);
    }

    /**
     * Фильтр по нижней границе даты проведения занятия включительно (lessonDate >= dateFrom).
     */
    public static Specification<Lesson> hasDateFrom(LocalDate dateFrom) {
        return (root, query, cb) -> dateFrom == null
                ? null
                : cb.greaterThanOrEqualTo(root.get("lessonDate"), dateFrom);
    }

    /**
     * Фильтр по верхней границе даты проведения занятия включительно (lessonDate <= dateTo).
     */
    public static Specification<Lesson> hasDateTo(LocalDate dateTo) {
        return (root, query, cb) -> dateTo == null
                ? null
                : cb.lessThanOrEqualTo(root.get("lessonDate"), dateTo);
    }
}
