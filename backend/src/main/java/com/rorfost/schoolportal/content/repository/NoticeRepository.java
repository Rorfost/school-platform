package com.rorfost.schoolportal.content.repository;

import com.rorfost.schoolportal.content.domain.Notice;
import com.rorfost.schoolportal.content.domain.PublicationStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, UUID> {
  Page<Notice> findBySchoolIdAndStatus(UUID schoolId, PublicationStatus status, Pageable pageable);

  java.util.Optional<Notice> findByIdAndSchoolId(UUID id, UUID schoolId);
}
