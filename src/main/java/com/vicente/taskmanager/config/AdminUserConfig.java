package com.vicente.taskmanager.config;

import com.vicente.taskmanager.model.entity.User;
import com.vicente.taskmanager.model.entity.UserRole;
import com.vicente.taskmanager.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Configuration
@Profile("dev")
public class AdminUserConfig implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;

    private final static Logger logger = LoggerFactory.getLogger(AdminUserConfig.class);

    public AdminUserConfig(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String @NonNull ... args) throws Exception {
        Optional<User> userAdmin = userRepository.findByEmail("system@admin.com");

                userAdmin.ifPresentOrElse(
                        admin -> logger.debug("[AdminUserConfig] Email '{}' is already registered.",
                                admin.getEmail()),
                () -> {
                    User admin = new User();
                    admin.setName("System");
                    admin.setEmail("system@admin.com");
                    admin.setPassword(passwordEncoder.encode(adminPassword));
                    admin.setEnabled(true);
                    admin.setAccountNonLocked(true);
                    admin.getRoles().add(UserRole.ADMIN);

                    userRepository.save(admin);
                    logger.debug("[AdminUserConfig] Admin registered successfully | adminId={}", admin.getId());
                });
    }
}
