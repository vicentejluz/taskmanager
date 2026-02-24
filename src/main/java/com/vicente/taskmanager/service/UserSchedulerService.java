package com.vicente.taskmanager.service;

public interface UserSchedulerService {
    void deleteDisabledUsersOlderThan180Days();
    void deleteUsersWithDeletedAtOlderThan180Days();
    void unlockUsersWithExpiredLock();
}
