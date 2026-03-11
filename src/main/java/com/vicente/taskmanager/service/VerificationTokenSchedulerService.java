package com.vicente.taskmanager.service;

public interface VerificationTokenSchedulerService {
    void deleteVerificationTokenTypeEmailExpiredBefore2Days();
    void deleteVerificationTokenTypePasswordExpiredBefore1Days();
}
