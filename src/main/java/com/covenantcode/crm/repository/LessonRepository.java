package com.covenantcode.crm.repository;

import com.covenantcode.crm.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long>, JpaSpecificationExecutor<Lesson> {

    List<Lesson> findAllByStudyGroupId(Long studyGroupId);

    List<Lesson> findByTeacherIdAndLessonDate(Long teacherId, LocalDate lessonDate);
}
