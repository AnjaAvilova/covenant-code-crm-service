package com.covenantcode.crm.repository;

import com.covenantcode.crm.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonRepository extends JpaRepository<Lesson, Long>, JpaSpecificationExecutor<Lesson> {

    List<Lesson> findAllByStudyGroupId(Long studyGroupId);

    List<Lesson> findByTeacherIdAndLessonDate(Long teacherId, LocalDate lessonDate);

    @Query("SELECT l FROM Lesson l WHERE l.studyGroup.id IN " +
            "  (SELECT g.id FROM StudyGroup g JOIN g.students s WHERE s.id = :studentId) " +
            "AND (CAST(:dateFrom AS date) IS NULL OR l.lessonDate >= :dateFrom) " +
            "AND (CAST(:dateTo AS date) IS NULL OR l.lessonDate <= :dateTo) " +
            "ORDER BY l.lessonDate ASC, l.startTime ASC")
    List<Lesson> findLessonsByStudentIdWithDates(
            @Param("studentId") Long studentId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo
    );
}
