package com.vicente.taskmanager.repository;

import com.vicente.taskmanager.domain.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    @Query("SELECT f FROM FileMetadata f WHERE f.task.id = :taskId AND f.status = 'ACTIVE'")
    List<FileMetadata> findAllActiveByTaskId(@Param("taskId") Long taskId);

    @Query("SELECT COUNT(f) FROM FileMetadata f WHERE f.task.id = :taskId AND f.status = 'ACTIVE'")
    Long countActiveByTaskId(@Param("taskId") Long taskId);

    @Query("SELECT COALESCE(SUM(f.size), 0) FROM FileMetadata f WHERE f.task.id = :taskId AND f.status = 'ACTIVE'")
    Long sumActiveSizeByTaskId(@Param("taskId") Long taskId);

    @Query("SELECT f FROM FileMetadata f WHERE f.id = :id AND f.status = 'ACTIVE'")
    Optional<FileMetadata> findActiveById(@Param("id") Long id);

    @Query("SELECT f FROM FileMetadata f WHERE f.storedFileName = :storedFileName AND f.status = 'ACTIVE'")
    Optional<FileMetadata> findActiveByStorageFileName(@Param("storedFileName") String storedFileName);
}
