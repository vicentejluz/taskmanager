package com.vicente.taskmanager.security.service;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordService implements PasswordEncoder {
    private final PasswordEncoder passwordEncoder;
    private final String PEPPER;

    public PasswordService(PasswordEncoder passwordEncoder, String pepper) {
        this.passwordEncoder = passwordEncoder;
        this.PEPPER = pepper;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return passwordEncoder.encode(applyPepper(rawPassword));
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return passwordEncoder.matches(applyPepper(rawPassword), encodedPassword);
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return passwordEncoder.upgradeEncoding(encodedPassword);
    }

    private String applyPepper(CharSequence rawPassword) {
        return rawPassword + PEPPER;
    }
}
