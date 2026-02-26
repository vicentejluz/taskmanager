package com.vicente.taskmanager.repository.specification;

import com.vicente.taskmanager.dto.filter.UserFilterDTO;
import com.vicente.taskmanager.model.entity.User;
import com.vicente.taskmanager.model.enums.AccountStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.Objects;

public final class UserSpecification {
    private UserSpecification() {}

    public static Specification<User> filter(UserFilterDTO userFilter) {
        return Specification
                .where(byDeletedAtIsNull())
                .and(byName(userFilter.name()))
                .and(byAccountStatus(userFilter.accountStatus()))
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

    private static Specification<User> byAccountStatus(String status) {
        return  (root, _, cb) -> {
            if(Objects.nonNull(status) && !status.isBlank()) {
                AccountStatus accountStatus = AccountStatus.convert(status.trim());
                return cb.equal(root.get("accountStatus"), accountStatus);
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

    private static Specification<User> byDeletedAtIsNull() {
        return  (root, _, cb) ->
                cb.isNull(root.get("deletedAt"));
    }
}
