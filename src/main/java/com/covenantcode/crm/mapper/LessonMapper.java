package com.covenantcode.crm.mapper;

import com.covenantcode.crm.dto.group.StudyGroupShortResponse;
import com.covenantcode.crm.dto.group.UserShortResponse;
import com.covenantcode.crm.dto.lesson.LessonCreateRequest;
import com.covenantcode.crm.dto.lesson.LessonResponse;
import com.covenantcode.crm.dto.lesson.LessonUpdateRequest;
import com.covenantcode.crm.entity.Lesson;
import com.covenantcode.crm.entity.StudyGroup;
import com.covenantcode.crm.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper
public interface LessonMapper {

    @Mapping(target = "studyGroup", source = "studyGroup")
    @Mapping(target = "teacher", source = "teacher")
    LessonResponse toResponse(Lesson lesson);

    List<LessonResponse> toResponseList(List<Lesson> lessons);

    UserShortResponse toUserShortResponse(User user);

    StudyGroupShortResponse toStudyGroupResponse(StudyGroup studyGroup);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "studyGroup", ignore = true)
    @Mapping(target = "teacher", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Lesson toEntity(LessonCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "studyGroup", ignore = true)
    @Mapping(target = "teacher", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(
            @MappingTarget Lesson lesson,
            LessonUpdateRequest request
    );
}
