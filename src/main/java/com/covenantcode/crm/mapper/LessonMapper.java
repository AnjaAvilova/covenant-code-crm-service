package com.covenantcode.crm.mapper;


import com.covenantcode.crm.dto.lesson.LessonCreateRequest;
import com.covenantcode.crm.dto.lesson.LessonResponse;
import com.covenantcode.crm.entity.Lesson;
import com.covenantcode.crm.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LessonMapper {

    @Mapping(target = "teacherId", source = "teacher.id")
    @Mapping(target = "teacherEmail", source = "teacher.email")
    @Mapping(target = "studyGroupId", source = "studyGroup.id")
    LessonResponse toResponse(Lesson lesson);

    List<LessonResponse> toResponseList(List<Lesson> lessons);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "teacher", source = "teacher")
    @Mapping(target = "topic", source = "request.topic")
    @Mapping(target = "description", source = "request.description")
    Lesson toEntity(LessonCreateRequest request, User teacher);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "teacher", source = "teacher")
    @Mapping(target = "topic", source = "request.topic")
    @Mapping(target = "description", source = "request.description")
    void updateEntity(@MappingTarget Lesson lesson, LessonCreateRequest request, User teacher);
}
