package com.rorfost.schoolportal.assessment.api;

import com.rorfost.schoolportal.assessment.application.ExamResultService;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

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
            @RequestParam(value = "class", required = false) String studentClass,
            @RequestParam("rollNumber") Integer rollNumber,
            @RequestParam(value = "resultType", defaultValue = ExamResultService.ANNUAL)
                    String resultType) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.getResult(standard, studentClass, rollNumber, resultType));
    }

    @PostMapping("/admin/exam-results/upload")
    public ResponseEntity<Map<String, String>> uploadResults(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "totalWorkingDays", required = false) String totalWorkingDays,
            @RequestParam(value = "resultType", defaultValue = ExamResultService.ANNUAL)
                    String resultType) {
        service.processExcelUpload(file, totalWorkingDays, resultType);
        return ResponseEntity.ok(Map.of("message", "Result uploaded successfully."));
    }

    @DeleteMapping("/admin/exam-results")
    public ResponseEntity<Map<String, String>> clearResults(
            @RequestParam(value = "resultType", defaultValue = ExamResultService.ANNUAL)
                    String resultType) {

        service.clearResults(resultType);

        return ResponseEntity.ok(Map.of("message", "Results cleared successfully."));
    }
}
