package com.rorfost.schoolportal.dashboard.api;

import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import com.rorfost.schoolportal.dashboard.application.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class DashboardController {
  private final DashboardService dashboardService;

  public DashboardController(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @GetMapping("/summary")
  public DashboardSummaryResponse summary(@AuthenticationPrincipal PrincipalSession principal) {
    return dashboardService.getSummary(principal.schoolId());
  }
}
