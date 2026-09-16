package com.rorfost.schoolportal.audit.api;

import com.rorfost.schoolportal.audit.service.AuditLogService;
import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
public class AuditLogController {
  private final AuditLogService auditLogService;

  public AuditLogController(AuditLogService auditLogService) {
    this.auditLogService = auditLogService;
  }

  @GetMapping
  public List<AuditLogResponse> list(@AuthenticationPrincipal PrincipalSession principal) {
    return auditLogService.list(principal.schoolId());
  }
}
