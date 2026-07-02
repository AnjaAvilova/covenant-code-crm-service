package com.covenantcode.crm.repository;

import com.covenantcode.crm.entity.LeadComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeadCommentRepository extends JpaRepository<LeadComment, Long> {

    List<LeadComment> findByLeadIdOrderByCreatedAtAsc(Long leadId);
}
