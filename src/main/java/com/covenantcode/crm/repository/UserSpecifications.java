package com.covenantcode.crm.repository;

import com.covenantcode.crm.entity.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class UserSpecifications {

    private UserSpecifications() {
        throw new IllegalStateException("Utility class");
    }

    public static Specification<User> searchByText(String search) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(search)) {
                return cb.conjunction();
            }

            String searchLower = "%" + search.trim().toLowerCase() + "%";

            Predicate firstName = cb.like(
                    cb.lower(root.get("firstName")),
                    searchLower
            );

            Predicate lastName = cb.like(
                    cb.lower(root.get("lastName")),
                    searchLower
            );

            Predicate email = cb.like(
                    cb.lower(root.get("email")),
                    searchLower
            );

            return cb.or(firstName, lastName, email);
        };
    }
}
