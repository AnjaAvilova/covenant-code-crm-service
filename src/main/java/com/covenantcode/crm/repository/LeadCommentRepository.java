package com.covenantcode.crm.repository;

import com.covenantcode.crm.entity.LeadComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadCommentRepository extends JpaRepository<LeadComment, Long> {
}
