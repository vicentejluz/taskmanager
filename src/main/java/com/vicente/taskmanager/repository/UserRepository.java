package com.vicente.taskmanager.repository;

import com.vicente.taskmanager.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String username);
    List<User> findByIsEnabledFalseAndUpdatedAtBefore(OffsetDateTime date);
    List<User> findByDeletedAtBefore(OffsetDateTime date);
    List<User> findByIsAccountNonLockedFalseAndLockTimeBefore(OffsetDateTime date);
}
