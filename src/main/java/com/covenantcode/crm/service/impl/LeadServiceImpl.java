package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.lead.LeadResponse;
import com.covenantcode.crm.dto.lead.LeadCommentCreateRequest;
import com.covenantcode.crm.dto.lead.LeadUpdateRequest;
import com.covenantcode.crm.dto.lead.LeadConvertRequest;
import com.covenantcode.crm.dto.lead.LeadCreateRequest;
import com.covenantcode.crm.dto.lead.LeadStatusUpdateRequest;
import com.covenantcode.crm.dto.lead.LeadCommentResponse;
import com.covenantcode.crm.dto.student.StudentResponse;
import com.covenantcode.crm.entity.Course;
import com.covenantcode.crm.entity.Lead;
import com.covenantcode.crm.entity.LeadComment;
import com.covenantcode.crm.entity.Student;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.LeadStatus;
import com.covenantcode.crm.exception.BadRequestException;
import com.covenantcode.crm.exception.ConflictException;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.mapper.LeadCommentMapper;
import com.covenantcode.crm.mapper.LeadMapper;
import com.covenantcode.crm.mapper.StudentMapper;
import com.covenantcode.crm.repository.LeadRepository;
import com.covenantcode.crm.repository.LeadCommentRepository;
import com.covenantcode.crm.repository.UserRepository;
import com.covenantcode.crm.repository.StudentRepository;
import com.covenantcode.crm.repository.LeadSpecifications;
import com.covenantcode.crm.repository.CourseRepository;

import com.covenantcode.crm.service.LeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;


@Service
@RequiredArgsConstructor
public class LeadServiceImpl implements LeadService {

    private final LeadRepository leadRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final LeadMapper leadMapper;
    private final LeadCommentRepository leadCommentRepository;
    private final LeadCommentMapper leadCommentMapper;

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;

    @Override
    @Transactional
    public LeadResponse create(LeadCreateRequest leadCreateRequest) {
        var course = leadCreateRequest.getInterestedCourseId() != null
                ? courseRepository.findById(leadCreateRequest.getInterestedCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", leadCreateRequest.getInterestedCourseId()))
                : null;

        var manager = leadCreateRequest.getAssignedManagerId() != null
                ? userRepository.findById(leadCreateRequest.getAssignedManagerId())
                .orElseThrow(() -> new ResourceNotFoundException("User", leadCreateRequest.getAssignedManagerId()))
                : null;

        Lead lead = new Lead();
        lead.setFirstName(leadCreateRequest.getFirstName());
        lead.setLastName(leadCreateRequest.getLastName());
        lead.setPhone(leadCreateRequest.getPhone());
        lead.setEmail(leadCreateRequest.getEmail());
        lead.setSource(leadCreateRequest.getSource());
        lead.setComment(leadCreateRequest.getComment());
        lead.setInterestedCourse(course);
        lead.setAssignedManager(manager);
        lead.setStatus(LeadStatus.NEW);

        Lead saved = leadRepository.save(lead);
        return leadMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public LeadResponse getById(Long id) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead с id " + id + " не найден"));
        LeadResponse response = leadMapper.toResponse(lead);
        response.setCommentsCount(0L);
        return response;
    }

    @Transactional(readOnly = true)
    public Page<LeadResponse> getAll(
            String search,
            LeadStatus status,
            Long assignedManagerId,
            Long interestedCourseId,
            Pageable pageable
    ) {
        Specification<Lead> spec = Specification.where(null);

        if (StringUtils.hasText(search)) {
            spec = spec.and(LeadSpecifications.searchByText(search));
        }

        if (status != null) {
            spec = spec.and(LeadSpecifications.hasStatus(status));
        }

        if (assignedManagerId != null) {
            spec = spec.and(LeadSpecifications.assignedToManager(assignedManagerId));
        }

        if (interestedCourseId != null) {
            spec = spec.and(LeadSpecifications.interestedInCourse(interestedCourseId));
        }

        return leadRepository.findAll(spec, pageable)
                .map(leadMapper::toResponse);
    }

    @Override
    @Transactional
    public StudentResponse convertToStudent(Long leadId, LeadConvertRequest request) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead с id " + leadId + " не найден"));

        if (lead.getStatus() == LeadStatus.CONVERTED_TO_STUDENT) {
            throw new ConflictException("Лид с id " + leadId + " уже был конвертирован в студента");
        }

        Student student = Student.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .birthDate(request.getBirthDate())
                .build();

        Student savedStudent = studentRepository.save(student);

        lead.setStatus(LeadStatus.CONVERTED_TO_STUDENT);
        lead.setConvertedStudent(savedStudent);
        leadRepository.save(lead);

        return studentMapper.toResponse(savedStudent);
    }

    @Override
    @Transactional
    public LeadCommentResponse addComment(Long leadId, LeadCommentCreateRequest request, Long authorId) {
        Lead lead = leadRepository.findById(leadId).orElseThrow(() -> new ResourceNotFoundException("Lead", leadId));

        User user = userRepository.findById(authorId).orElseThrow(() -> new ResourceNotFoundException("User", authorId));

        LeadComment leadComment = LeadComment.builder()
                .lead(lead)
                .author(user)
                .text(request.getText())
                .build();

        LeadComment savedComment = leadCommentRepository.save(leadComment);

        return leadCommentMapper.toResponse(savedComment);
    }

    @Transactional
    @Override
    public LeadResponse update(Long id, LeadUpdateRequest request) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead с id " + id + " не найден"));

        if (lead.getStatus() == LeadStatus.CONVERTED_TO_STUDENT) {
            throw new BadRequestException("Нельзя редактировать конвертированного лида");
        }

        if (request.getInterestedCourseId() != null) {
            Course course = courseRepository.findById(request.getInterestedCourseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course с id " + request.getInterestedCourseId() + " не найден"));
            lead.setInterestedCourse(course);
        } else {
            lead.setInterestedCourse(null);
        }

        if (request.getAssignedManagerId() != null) {
            User manager = userRepository.findById(request.getAssignedManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User с id " + request.getAssignedManagerId() + " не найден"));
            lead.setAssignedManager(manager);
        } else {
            lead.setAssignedManager(null);
        }

        lead.setFirstName(request.getFirstName());
        lead.setLastName(request.getLastName());
        lead.setPhone(request.getPhone());
        lead.setEmail(request.getEmail());
        lead.setSource(request.getSource());
        lead.setComment(request.getComment());

        Lead updatedLead = leadRepository.save(lead);

        return leadMapper.toResponse(updatedLead);
    }

    @Override
    public LeadResponse updateStatus(Long id, LeadStatusUpdateRequest request) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead с id " + id + " не найден"));

        if (request.getStatus() == LeadStatus.CONVERTED_TO_STUDENT) {
            throw new ConflictException(
                    "Статус CONVERTED_TO_STUDENT нельзя установить вручную. Используйте POST /api/v1/leads/%d/convert".formatted(id)
            );
        }

        lead.setStatus(request.getStatus());
        Lead updatedLead = leadRepository.save(lead);
        return leadMapper.toResponse(updatedLead);
    }


    @Transactional(readOnly = true)
    @Override
    public List<LeadCommentResponse> getComments(Long leadId) {
        if (!leadRepository.existsById(leadId)) {
            throw new ResourceNotFoundException("Lead с id " + leadId + " не найден");
        }
        return leadCommentRepository.findByLeadIdOrderByCreatedAtAsc(leadId)
                .stream()
                .map(leadCommentMapper::toResponse)
                .toList();
    }
}


