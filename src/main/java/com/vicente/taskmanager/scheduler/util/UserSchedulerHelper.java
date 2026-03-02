package com.vicente.taskmanager.scheduler.util;

import com.vicente.taskmanager.model.entity.User;
import com.vicente.taskmanager.model.enums.AccountStatus;
import com.vicente.taskmanager.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserSchedulerHelper {
    private final UserRepository userRepository;
    private final Logger logger = LoggerFactory.getLogger(UserSchedulerHelper.class);

    public UserSchedulerHelper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteSingleUser(User user) {
        logger.debug("[USER SCHEDULER] Executing Delete single user | userId={}", user.getId());
        userRepository.delete(user);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resolvePendingVerification(User user) {
        logger.debug("[USER SCHEDULER] Executing resolve pending verification | userId={}", user.getId());
        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);
    }
}
