package com.vicente.taskmanager.service;

public interface RefreshTokenSchedulerService {
    void deleteRefreshTokensExpiredBefore7Days();
    void deleteRefreshTokensOfUsersDeletedBefore3Days();
}
