package com.covenantcode.crm.service;

import com.covenantcode.crm.dto.group.AddStudentToGroupRequest;
import com.covenantcode.crm.dto.group.GroupStatusUpdateRequest;
import com.covenantcode.crm.dto.group.StudyGroupCreateRequest;
import com.covenantcode.crm.dto.group.StudyGroupResponse;
import com.covenantcode.crm.dto.group.StudyGroupUpdateRequest;
import com.covenantcode.crm.dto.lesson.LessonResponse;
import com.covenantcode.crm.dto.student.StudentResponse;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.GroupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;


public interface StudyGroupService{

    StudyGroupResponse create(StudyGroupCreateRequest request);

    Page<StudyGroupResponse> getAll(Long courseId, Long teacherId, GroupStatus status, Pageable pageable);

    StudyGroupResponse updateStatus(Long id, GroupStatusUpdateRequest request);

    StudyGroupResponse update(Long id, StudyGroupUpdateRequest request);

    List<StudentResponse> getGroupStudents(Long id);

    List<LessonResponse> getGroupLessons(Long id);

    StudyGroupResponse getById(Long id, User currentUser);

    void removeStudent(Long groupId, Long studentId);

    StudyGroupResponse addStudent(Long groupId, AddStudentToGroupRequest request);

}
