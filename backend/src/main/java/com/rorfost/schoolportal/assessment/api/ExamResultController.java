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
      @RequestParam("standard") String standard,
      @RequestParam("rollNumber") Integer rollNumber,
      @RequestParam(value = "resultType", defaultValue = ExamResultService.ANNUAL)
          String resultType) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(service.getResult(standard, rollNumber, resultType));
  }

  @PostMapping("/admin/exam-results/upload")
  public ResponseEntity<Map<String, String>> uploadResults(
      @RequestParam("file") MultipartFile file,
      @RequestParam("totalWorkingDays") Integer totalWorkingDays,
      @RequestParam(value = "resultType", defaultValue = ExamResultService.ANNUAL)
          String resultType) {
    service.processExcelUpload(file, totalWorkingDays, resultType);
    return ResponseEntity.ok(Map.of("message", "Result uploaded successfully."));
  }
}
