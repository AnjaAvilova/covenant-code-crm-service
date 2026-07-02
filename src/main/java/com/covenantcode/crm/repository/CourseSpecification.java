package com.covenantcode.crm.repository;

import com.covenantcode.crm.entity.Course;
import com.covenantcode.crm.entity.enums.CourseStatus;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;


import java.util.ArrayList;
import java.util.List;

public class CourseSpecification {

    private CourseSpecification() {
    }

    public static Specification<Course> withFilters(String search, CourseStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";

                Predicate titleLike = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descriptionLike = cb.like(cb.lower(root.get("description")), pattern);

                predicates.add(cb.or(titleLike, descriptionLike));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
