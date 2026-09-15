package com.rorfost.schoolportal.content.repository;

import com.rorfost.schoolportal.content.domain.Download;
import com.rorfost.schoolportal.content.domain.PublicationStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DownloadRepository extends JpaRepository<Download, UUID> {
  Page<Download> findBySchoolIdAndStatus(
      UUID schoolId, PublicationStatus status, Pageable pageable);

  java.util.Optional<Download> findByIdAndSchoolId(UUID id, UUID schoolId);
}
