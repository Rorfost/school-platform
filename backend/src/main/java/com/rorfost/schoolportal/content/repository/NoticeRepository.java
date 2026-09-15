package com.rorfost.schoolportal.content.repository;

import com.rorfost.schoolportal.content.domain.Notice;
import com.rorfost.schoolportal.content.domain.PublicationStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeRepository extends JpaRepository<Notice, UUID> {
  @Query(
      """
      select notice from Notice notice
      where notice.schoolId = :schoolId
        and notice.status = :status
        and (notice.expiresAt is null or notice.expiresAt > CURRENT_TIMESTAMP)
      """)
  Page<Notice> findVisibleBySchoolIdAndStatus(
      @Param("schoolId") UUID schoolId,
      @Param("status") PublicationStatus status,
      Pageable pageable);

  java.util.Optional<Notice> findByIdAndSchoolId(UUID id, UUID schoolId);
}
