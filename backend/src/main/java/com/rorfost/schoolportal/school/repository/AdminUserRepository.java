package com.rorfost.schoolportal.school.repository;

import com.rorfost.schoolportal.school.domain.AdminUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {
  Optional<AdminUser> findBySchoolIdAndEmail(UUID schoolId, String email);
}
