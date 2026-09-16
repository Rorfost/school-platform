package com.rorfost.schoolportal.academic.api;

import com.rorfost.schoolportal.academic.application.StudentService;
import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/academic/students")
public class StudentAdminController {
  private final StudentService studentService;

  public StudentAdminController(StudentService studentService) {
    this.studentService = studentService;
  }

  @GetMapping
  public List<StudentResponse> list(
      @AuthenticationPrincipal PrincipalSession principal, @RequestParam UUID standardId) {
    return studentService.listByStandard(principal.schoolId(), standardId);
  }

  @PostMapping
  public StudentResponse create(
      @AuthenticationPrincipal PrincipalSession principal,
      @Valid @RequestBody StudentRequest request) {
    return studentService.createStudent(principal.schoolId(), request);
  }

  @PutMapping("/{id}")
  public StudentResponse update(
      @AuthenticationPrincipal PrincipalSession principal,
      @PathVariable UUID id,
      @Valid @RequestBody StudentRequest request) {
    return studentService.updateStudent(principal.schoolId(), id, request);
  }
}
