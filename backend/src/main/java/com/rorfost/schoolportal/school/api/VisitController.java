package com.rorfost.schoolportal.school.api;

import com.rorfost.schoolportal.school.application.VisitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/visits")
class VisitController {
  private final VisitService visits;

  VisitController(VisitService visits) {
    this.visits = visits;
  }

  @GetMapping
  VisitResponse get() {
    return visits.get();
  }

  @PostMapping
  VisitResponse increment() {
    return visits.increment();
  }
}
