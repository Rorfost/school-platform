package com.rorfost.schoolportal.assessment.api;

import com.rorfost.schoolportal.assessment.application.ExamResultService;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class ExamResultController {

  private final ExamResultService service;

  public ExamResultController(ExamResultService service) {
    this.service = service;
  }

  @GetMapping("/public/exam-results")
  public ResponseEntity<ExamResultResponse> getResult(
      @RequestParam("standard") String standard, @RequestParam("rollNumber") Integer rollNumber) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(service.getResult(standard, rollNumber));
  }

  @PostMapping("/admin/exam-results/upload")
  public ResponseEntity<?> uploadResults(
      @RequestParam("file") MultipartFile file,
      @RequestParam("totalWorkingDays") Integer totalWorkingDays) {
    service.processAnnualExcelUpload(file, totalWorkingDays);
    return ResponseEntity.ok(Map.of("message", "Upload successful"));
  }

  @PostMapping("/admin/exam-results/ekam-kasoti/upload")
  public ResponseEntity<Map<String, String>> uploadEkamKasoti(@RequestParam("file") MultipartFile file) {
    service.processEkamKasotiUpload(file);
    return ResponseEntity.ok(Map.of("message", "એકમ કસોટીનું પરિણામ સફળતાપૂર્વક અપલોડ થયું."));
  }
}
