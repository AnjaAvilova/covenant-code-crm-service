package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.course.CourseCreateRequest;
import com.covenantcode.crm.dto.course.CourseResponse;
import com.covenantcode.crm.dto.course.CourseUpdateRequest;
import com.covenantcode.crm.entity.Course;
import com.covenantcode.crm.entity.enums.CourseStatus;
import com.covenantcode.crm.entity.enums.GroupStatus;
import com.covenantcode.crm.exception.ConflictException;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.mapper.CourseMapper;
import com.covenantcode.crm.repository.CourseRepository;
import com.covenantcode.crm.repository.CourseSpecification;
import com.covenantcode.crm.repository.StudyGroupRepository;
import com.covenantcode.crm.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;
    private final CourseRepository courseRepository;
    private final StudyGroupRepository studyGroupRepository;

    @Override
    @Transactional
    public CourseResponse create(CourseCreateRequest request) {
        Course course = courseMapper.toEntity(request);

        if (course.getStatus() == null) {
            course.setStatus(CourseStatus.ACTIVE);
        }

        Course savedCourse = courseRepository.save(course);
        return courseMapper.toResponse(savedCourse);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));
        return courseMapper.toResponse(course);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Course course = courseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Course", id));

        boolean hasActiveGroups = studyGroupRepository.existsByCourseIdAndStatus(id, GroupStatus.ACTIVE);

        if (hasActiveGroups) {
            throw new ConflictException("Невозможно удалить курс: существуют активные учебные группы");
        }

        courseRepository.deleteById(id);
    }

    @Override
    @Transactional
    public CourseResponse update(Long id, CourseUpdateRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));

        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setDurationInWeeks(request.getDurationInWeeks());
        course.setPrice(request.getPrice());
        course.setStatus(request.getStatus());

        Course savedCourse = courseRepository.save(course);
        return courseMapper.toResponse(savedCourse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseResponse> getAll(String search, CourseStatus status, Pageable pageable) {
        Specification<Course> specification = CourseSpecification.withFilters(search, status);

        return courseRepository.findAll(specification, pageable)
                .map(courseMapper::toResponse);
    }
}
