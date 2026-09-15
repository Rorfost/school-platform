package com.rorfost.schoolportal.content.repository;

import com.rorfost.schoolportal.content.domain.Notice;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, UUID> {}
