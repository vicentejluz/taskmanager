package com.vicente.taskmanager.service;

public interface TaskSchedulerService {
    void updateOverdueTasks();
    void deleteCancelledTasksOlderThan90Days();
    void deleteDoneTasksOlderThan180Days();
    boolean hasTasks();
}
