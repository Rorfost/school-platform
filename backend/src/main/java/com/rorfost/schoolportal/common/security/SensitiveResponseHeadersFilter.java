package com.rorfost.schoolportal.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SensitiveResponseHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        boolean isMutation = !"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method);

        if (uri.startsWith("/api/v1/admin/")
                || uri.equals("/api/v1/public/exam-results")
                || uri.startsWith("/api/v1/public/visits")
                || isMutation) {
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Pragma", "no-cache");
        }
        filterChain.doFilter(request, response);
    }
}
