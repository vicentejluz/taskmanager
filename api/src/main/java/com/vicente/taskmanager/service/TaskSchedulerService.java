package com.vicente.taskmanager.service;

public interface TaskSchedulerService {
    void updateOverdueTasks(String source);
    void deleteCancelledTasksOlderThan90Days(String source);
    void deleteDoneTasksOlderThan180Days(String source);
}
