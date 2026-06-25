package com.covenantcode.crm.service;


import com.covenantcode.crm.dto.lead.LeadCommentCreateRequest;
import com.covenantcode.crm.dto.lead.LeadCommentResponse;
import com.covenantcode.crm.dto.lead.LeadConvertRequest;
import com.covenantcode.crm.dto.lead.LeadCommentCreateRequest;
import com.covenantcode.crm.dto.lead.LeadCommentResponse;
import com.covenantcode.crm.dto.lead.LeadCreateRequest;
import com.covenantcode.crm.dto.lead.LeadResponse;
import com.covenantcode.crm.dto.student.StudentResponse;
import com.covenantcode.crm.entity.enums.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LeadService {

    LeadResponse create(LeadCreateRequest leadCreateRequest);

    LeadResponse getById(Long id);

    Page<LeadResponse> getAll(
            String search,          // Поиск по firstName, lastName, phone, email
            LeadStatus status,
            Long assignedManagerId, // вместо managerId
            Long interestedCourseId, // новый параметр
            Pageable pageable
    );

    LeadCommentResponse addComment(Long leadId, LeadCommentCreateRequest request, Long authorId);

    StudentResponse convertToStudent(Long leadId, LeadConvertRequest request);
}
