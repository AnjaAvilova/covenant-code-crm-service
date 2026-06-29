package com.covenantcode.crm.repository;

import com.covenantcode.crm.entity.LeadComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeadCommentRepository extends JpaRepository<LeadComment, Long> {
    /**
     * Находит комментарии по ID лида, отсортированные по дате создания в порядке возрастания (от старых к новым)
     * @param leadId ID лида
     * @return Список комментариев, отсортированных по дате создания (ASC)
     */
    List<LeadComment> findByLeadIdOrderByCreatedAtAsc(Long leadId);
}
