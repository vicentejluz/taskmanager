package com.vicente.taskmanager.repository.specification;

import com.vicente.taskmanager.dto.filter.UserFilterDTO;
import com.vicente.taskmanager.model.entity.User;
import org.springframework.data.jpa.domain.Specification;

import java.util.Objects;

public final class UserSpecification {
    private UserSpecification() {}

    public static Specification<User> filter(UserFilterDTO userFilter) {
        return Specification
                .where(byName(userFilter.name()))
                .and(byEnabled(userFilter.isEnabled()))
                .and(byAccountNonLocked(userFilter.isAccountNonLocked()));
    }

    private static Specification<User> byName(String name) {
        return  (root, _, cb) -> {
            if(Objects.nonNull(name) && !name.isBlank()) {
                String like = "%" + name.trim().toLowerCase() + "%";
                return cb.like(cb.lower(root.get("name")), like);
            }

            return null;
        };
    }

    private static Specification<User> byEnabled(Boolean isEnabled) {
        return  (root, _, cb) -> {
            if(Objects.nonNull(isEnabled)) {
                return cb.equal(root.get("isEnabled"), isEnabled);
            }

            return null;
        };
    }

    private static Specification<User> byAccountNonLocked(Boolean isAccountNonLocked) {
        return  (root, _, cb) -> {
            if(Objects.nonNull(isAccountNonLocked)) {
                return cb.equal(root.get("isAccountNonLocked"), isAccountNonLocked);
            }

            return null;
        };
    }
}
