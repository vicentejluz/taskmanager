package com.vicente.taskmanager.domain.entity;

import com.vicente.taskmanager.domain.entity.base.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "tb_file_metadata")
public class FileMetadata extends BaseEntity {
    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private String extension;

    @Column(name = "stored_file_name", nullable = false)
    private String storedFileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long size;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    public FileMetadata() {
    }

    public FileMetadata(String fileName, String path, String extension, String storedFileName, String contentType,
                        Long size, Task task) {
        this.fileName = fileName;
        this.path = path;
        this.extension = extension;
        this.contentType = contentType;
        this.storedFileName = storedFileName;
        this.size = size;
        this.task = task;
    }

    public String getFileName() {
        return fileName;
    }

    public String getPath() {
        return path;
    }

    public String getExtension() {
        return extension;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getSize() {
        return size;
    }

    public Task getTask() {
        return task;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public void setTask(Task task) {
        this.task = task;
    }
}
