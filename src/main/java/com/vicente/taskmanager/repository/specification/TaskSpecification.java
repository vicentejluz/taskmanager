package com.vicente.taskmanager.repository.specification;

import com.vicente.taskmanager.model.entity.Task;
import com.vicente.taskmanager.model.enums.TaskStatus;
import com.vicente.taskmanager.dto.filter.TaskFilterDTO;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Objects;

public final class TaskSpecification {
    private TaskSpecification() {}

    public static Specification<Task> filter(TaskFilterDTO taskFilter) {
        return Specification
                .where(belongsToUser(taskFilter.userId()))
                .and(byStatus(taskFilter.status()))
                .and(byDueDate(taskFilter.dueDate()));
    }

    private static Specification<Task> byStatus(TaskStatus status) {
        return (root, _, cb) -> {

            if (Objects.nonNull(status)) {
                return cb.equal(root.get("status"), status);
            }

            return null;
        };
    }

    private static Specification<Task> byDueDate(LocalDate dueDate) {
        return (root, _, cb) -> {

            if (Objects.nonNull(dueDate)) {
                return cb.equal(root.get("dueDate"), dueDate);
            }

            return null;
        };
    }

    private static Specification<Task> belongsToUser(Long userId) {
        return (root, _, cb) ->
                cb.equal(root.get("user").get("id"), userId);
    }
}
