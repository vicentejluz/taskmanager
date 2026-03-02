package com.vicente.taskmanager.service.impl;

import com.vicente.taskmanager.model.entity.User;
import com.vicente.taskmanager.model.enums.AccountStatus;
import com.vicente.taskmanager.repository.UserRepository;
import com.vicente.taskmanager.scheduler.util.UserSchedulerHelper;
import com.vicente.taskmanager.service.UserSchedulerService;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional(readOnly = true)
    public void deleteDisabledUsersOlderThan180Days() {
        OffsetDateTime thresholdDate = OffsetDateTime.now().minusDays(180);
        List<User> users = userRepository.findByAccountStatusAndUpdatedAtBefore(
                AccountStatus.DISABLED_BY_ADMIN,
                thresholdDate);

        deleteUsers(users, thresholdDate);
    }

    @Override
    @Transactional(readOnly = true)
    public void deleteUsersWithDeletedAtOlderThan180Days() {
        OffsetDateTime thresholdDate = OffsetDateTime.now().minusDays(180);
        List<User> users = userRepository.findByDeletedAtBefore(thresholdDate);

        deleteUsers(users, thresholdDate);
    }

    @Override
    @Transactional(readOnly = true)
    public void deleteUsersWithPendingVerificationOlderThan72Hours() {
        OffsetDateTime thresholdDate = OffsetDateTime.now().minusHours(72);
        List<User> users = userRepository.findByAccountStatusAndUpdatedAtBefore(
                AccountStatus.PENDING_VERIFICATION,
                thresholdDate);

        deleteUsersWithStatusPending(users, thresholdDate);
    }

    private void deleteUsers(List<User> users, OffsetDateTime thresholdDate) {
        if (!users.isEmpty()) {
            AtomicInteger count = new AtomicInteger();
            users.forEach(user -> {
                try {
                    userSchedulerHelper.deleteSingleUser(user);
                    count.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                    logger.warn("[USER SCHEDULER] User skipped due to optimistic lock - deleteUsers" +
                            " | userId={}", user.getId());
                }
            });
            if(count.get() > 0) {
                logger.info("[USER SCHEDULER] Users deleted | threshold={} count={}", thresholdDate, count.get());
                return;
            }
            logger.warn("[USER SCHEDULER] Users found but none deleted due to concurrency");
        }else{
            logger.debug("[USER SCHEDULER] No users to delete");
        }
    }

    private void deleteUsersWithStatusPending(List<User> users, OffsetDateTime thresholdDate) {
        if (!users.isEmpty()) {
            AtomicInteger count = new AtomicInteger();
            users.forEach(user -> {
                try {
                    if (user.getDeletedAt() == null) {
                        userSchedulerHelper.deleteSingleUser(user);
                    }else{
                        userSchedulerHelper.resolvePendingVerification(user);
                    }
                    count.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                    logger.warn("[USER SCHEDULER] User skipped due to optimistic lock - deleteUsersWithStatusPending" +
                            " | userId={}", user.getId());
                }
            });
            if(count.get() > 0) {
                logger.info("[USER SCHEDULER] Users deleted or resolved pending verification | threshold={} count={}",
                        thresholdDate, count.get());
                return;
            }
            logger.warn("[USER SCHEDULER] Users found but none deleted resolved pending verification due to concurrency");
        }else{
            logger.debug("[USER SCHEDULER] No users to delete or resolved pending verification");
        }
    }
}
