package com.rorfost.schoolportal.auth.api;

public record CsrfTokenResponse(String token, String headerName) {}
