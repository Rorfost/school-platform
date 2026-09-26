package com.rorfost.schoolportal.school.api;

import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import com.rorfost.schoolportal.school.application.VisitService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AdminVisitController {
  private final VisitService visits;

  AdminVisitController(VisitService visits) {
    this.visits = visits;
  }

  @PostMapping("/api/v1/admin/site-metrics/visits/reset")
  VisitResponse reset(@AuthenticationPrincipal PrincipalSession principal) {
    return visits.reset(principal.schoolId());
  }
}
