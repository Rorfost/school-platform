package com.rorfost.schoolportal.auth.application;

import com.rorfost.schoolportal.common.config.AdminBootstrapProperties;
import com.rorfost.schoolportal.school.domain.AdminUser;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.AdminUserRepository;
import com.rorfost.schoolportal.school.repository.SchoolRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
public class AdminBootstrapService implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminBootstrapService.class);

    private final AdminBootstrapProperties properties;
    private final AdminUserRepository adminUserRepository;
    private final SchoolRepository schoolRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapService(
            AdminBootstrapProperties properties,
            AdminUserRepository adminUserRepository,
            SchoolRepository schoolRepository,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.adminUserRepository = adminUserRepository;
        this.schoolRepository = schoolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        initialize();
    }

    @Transactional
    public void initialize() {
        if (!properties.isConfigured()) {
            return;
        }

        School school = schoolRepository.findBySlug(properties.schoolSlug()).orElse(null);
        if (school == null) {
            logger.warn(
                    "Initial administrator bootstrap skipped because its school is unavailable");
            return;
        }

        String normalizedEmail = normalizeEmail(properties.initialEmail());
        if (adminUserRepository
                .findBySchoolIdAndEmail(school.getId(), normalizedEmail)
                .isPresent()) {
            return;
        }

        adminUserRepository.save(
                new AdminUser(
                        school.getId(),
                        normalizedEmail,
                        passwordEncoder.encode(properties.initialPassword())));
        logger.info(
                "Initial principal account created for school slug={}", properties.schoolSlug());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
