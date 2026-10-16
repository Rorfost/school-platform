package com.rorfost.schoolportal.school.repository;

import com.rorfost.schoolportal.school.domain.AdminUser;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {
    Optional<AdminUser> findBySchoolIdAndEmail(UUID schoolId, String email);

    List<AdminUser> findAllByEmail(String email);
}
