package com.rorfost.schoolportal.academic.api;

import jakarta.validation.constraints.Size;

public record ClassTeacherRequest(@Size(max = 160) String classTeacherName) {}
