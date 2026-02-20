package com.vicente.taskmanager.service;

public interface UserSchedulerService {
    void deleteDisabledUsersOlderThan180Days();
    void unlockUsersWithExpiredLock();
}
