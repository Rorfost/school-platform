package com.rorfost.schoolportal.content.repository;

import com.rorfost.schoolportal.content.domain.Download;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DownloadRepository extends JpaRepository<Download, UUID> {}
