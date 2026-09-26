package com.rorfost.schoolportal.school.application;

import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.school.api.VisitResponse;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VisitService {
  private final JdbcTemplate jdbc;
  private final SchoolRepository schools;

  public VisitService(JdbcTemplate jdbc, SchoolRepository schools) {
    this.jdbc = jdbc;
    this.schools = schools;
  }

  @Transactional
  public VisitResponse increment() {
    UUID schoolId = publicSchoolId();
    // PostgreSQL performs the increment inside one UPSERT, so concurrent page opens cannot lose
    // visits.
    jdbc.update(
        """
        INSERT INTO site_metrics (school_id, total_visits) VALUES (?, 1)
        ON CONFLICT (school_id) DO UPDATE
        SET total_visits = site_metrics.total_visits + 1, updated_at = CURRENT_TIMESTAMP
        """,
        schoolId);
    return count(schoolId);
  }

  @Transactional(readOnly = true)
  public VisitResponse get() {
    return count(publicSchoolId());
  }

  @Transactional
  public VisitResponse reset(UUID schoolId) {
    jdbc.update(
        """
        INSERT INTO site_metrics (school_id, total_visits) VALUES (?, 0)
        ON CONFLICT (school_id) DO UPDATE
        SET total_visits = 0, updated_at = CURRENT_TIMESTAMP
        """,
        schoolId);
    return count(schoolId);
  }

  private VisitResponse count(UUID schoolId) {
    Long count =
        jdbc.queryForObject(
            "SELECT COALESCE((SELECT total_visits FROM site_metrics WHERE school_id = ?), 0)",
            Long.class,
            schoolId);
    return new VisitResponse(count == null ? 0 : count);
  }

  private UUID publicSchoolId() {
    return schools
        .findFirstByIsActiveTrueOrderByCreatedAtAsc()
        .map(s -> s.getId())
        .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "school_not_configured"));
  }
}
