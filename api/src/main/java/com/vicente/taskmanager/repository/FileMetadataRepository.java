package com.vicente.taskmanager.repository;

import com.vicente.taskmanager.domain.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    List<FileMetadata> findAllByTaskId(Long taskId);

    @Query("SELECT COUNT(f) FROM FileMetadata f WHERE f.task.id = :taskId AND f.status = 'ACTIVE'")
    Long countAllByTaskId(@Param("taskId") Long taskId);

    @Query("SELECT COALESCE(SUM(f.size), 0) FROM FileMetadata f WHERE f.task.id = :taskId AND f.status = 'ACTIVE'")
    Long sumAllByTaskId(Long taskId);
}
