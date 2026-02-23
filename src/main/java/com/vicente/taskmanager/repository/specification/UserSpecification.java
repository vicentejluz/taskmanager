package com.vicente.taskmanager.repository.specification;

import com.vicente.taskmanager.dto.filter.UserFilterDTO;
import com.vicente.taskmanager.model.entity.User;
import org.springframework.data.jpa.domain.Specification;

import java.util.Objects;

public final class UserSpecification {
    private UserSpecification() {}

    public static Specification<User> filter(UserFilterDTO userFilter) {
        return Specification
                .where(byDeleteAtIsNull())
                .and(byName(userFilter.name()))
                .and(byEnabled(userFilter.enabled()))
                .and(byAccountNonLocked(userFilter.accountNonLocked()));
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

    private static Specification<User> byEnabled(Boolean enabled) {
        return  (root, _, cb) -> {
            if(Objects.nonNull(enabled)) {
                return cb.equal(root.get("isEnabled"), enabled);
            }

            return null;
        };
    }

    private static Specification<User> byAccountNonLocked(Boolean accountNonLocked) {
        return  (root, _, cb) -> {
            if(Objects.nonNull(accountNonLocked)) {
                return cb.equal(root.get("isAccountNonLocked"), accountNonLocked);
            }

            return null;
        };
    }

    private static Specification<User> byDeleteAtIsNull() {
        return  (root, _, cb) ->
                cb.isNull(root.get("deleteAt"));
    }
}
