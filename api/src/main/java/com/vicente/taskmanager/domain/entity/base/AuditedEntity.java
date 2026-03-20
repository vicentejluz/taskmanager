package com.vicente.taskmanager.domain.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;

@MappedSuperclass
public abstract class AuditedEntity extends BaseEntity {
    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public Long getVersion() {
        return version;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
