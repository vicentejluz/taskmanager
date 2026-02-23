package com.vicente.taskmanager.service.impl;

import com.vicente.taskmanager.model.entity.User;
import com.vicente.taskmanager.repository.UserRepository;
import com.vicente.taskmanager.scheduler.util.UserSchedulerHelper;
import com.vicente.taskmanager.service.UserSchedulerService;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class UserSchedulerServiceImpl implements UserSchedulerService {
    private final UserRepository userRepository;
    private final UserSchedulerHelper userSchedulerHelper;
    private static final Logger logger = LoggerFactory.getLogger(UserSchedulerServiceImpl.class);

    public UserSchedulerServiceImpl(UserRepository userRepository, UserSchedulerHelper userSchedulerHelper) {
        this.userRepository = userRepository;
        this.userSchedulerHelper = userSchedulerHelper;
    }

    @Override
    public void deleteDisabledUsersOlderThan180Days() {
        OffsetDateTime thresholdDate = OffsetDateTime.now().minusDays(180);
        List<User> users = userRepository.findByIsEnabledFalseAndUpdatedAtBefore(thresholdDate);

        deleteUsers(users, thresholdDate);
    }

    @Override
    public void deleteUsersWithDeleteAtOlderThan180Days() {
        OffsetDateTime thresholdDate = OffsetDateTime.now().minusDays(180);
        List<User> users = userRepository.findByDeletedAtBefore(thresholdDate);

        deleteUsers(users, thresholdDate);
    }

    private void deleteUsers(List<User> users, OffsetDateTime thresholdDate) {
        if (!users.isEmpty()) {
            AtomicInteger count = new AtomicInteger();
            users.forEach(user -> {
                try {
                    userSchedulerHelper.deleteSingleUser(user);
                    count.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                    logger.warn("[USER SCHEDULER] User skipped due to optimistic lock | userId={}", user.getId());
                }
            });
            if(count.get() > 0) {
                logger.info("[USER SCHEDULER] Users deleted | threshold={} count={}", thresholdDate, count.get());
                return;
            }
            logger.warn("[USER SCHEDULER] Users found but none deleted due to concurrency");
        }else{
            logger.debug("[USER SCHEDULER] No Users to delete");
        }
    }

    @Override
    public void unlockUsersWithExpiredLock() {
        List<User> users = userRepository.findByIsAccountNonLockedFalseAndLockTimeBefore(OffsetDateTime.now());

        if (!users.isEmpty()) {
            AtomicInteger count = new AtomicInteger();
            users.forEach(user -> {
                try {
                    userSchedulerHelper.unlockSingleUser(user);
                    count.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                    logger.warn("[USER SCHEDULER] User skipped due to optimistic lock | userId={}", user.getId());
                }
            });
            if(count.get() > 0) {
                logger.info("[USER SCHEDULER] Users update | count={}", count.get());
                return;
            }
            logger.warn("[USER SCHEDULER] Users found but none updated due to concurrency");
        }else{
            logger.debug("[USER SCHEDULER] No Users to updated");
        }
    }
}
