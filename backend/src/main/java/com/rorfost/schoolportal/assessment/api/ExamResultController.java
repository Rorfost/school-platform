package com.rorfost.schoolportal.assessment.api;

import com.rorfost.schoolportal.assessment.application.ExamResultService;
import java.util.Map;
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
  public ExamResultResponse getResult(
      @RequestParam("standard") String standard, @RequestParam("rollNumber") Integer rollNumber) {
    return service.getResult(standard, rollNumber);
  }

  @PostMapping("/admin/exam-results/upload")
  public ResponseEntity<?> uploadResults(
      @RequestParam("file") MultipartFile file,
      @RequestParam("totalWorkingDays") Integer totalWorkingDays) {
    service.processExcelUpload(file, totalWorkingDays);
    return ResponseEntity.ok(Map.of("message", "Upload successful"));
  }
}
