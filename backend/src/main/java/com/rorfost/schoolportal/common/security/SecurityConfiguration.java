package com.rorfost.schoolportal.common.security;

import com.rorfost.schoolportal.common.config.CorsProperties;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      AuthenticationEntryPoint authenticationEntryPoint,
      AccessDeniedHandler accessDeniedHandler)
      throws Exception {
    http.cors(Customizer.withDefaults())
        .csrf(
            csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .ignoringRequestMatchers("/actuator/health"))
        .sessionManagement(
            sessions ->
                sessions
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .sessionFixation(sessionFixation -> sessionFixation.changeSessionId()))
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .headers(
            headers ->
                headers
                    .contentSecurityPolicy(
                        policy ->
                            policy.policyDirectives(
                                "default-src 'none'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'"))
                    .frameOptions(frameOptions -> frameOptions.deny())
                    .referrerPolicy(
                        referrer ->
                            referrer.policy(
                                org.springframework.security.web.header.writers
                                    .ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                    .permissionsPolicyHeader(
                        permissions ->
                            permissions.policy("geolocation=(), microphone=(), camera=()"))
                    .httpStrictTransportSecurity(
                        hsts ->
                            hsts.includeSubDomains(true).preload(true).maxAgeInSeconds(31536000)))
        .authorizeHttpRequests(
            authorization ->
                authorization
                    .requestMatchers("/actuator/health")
                    .permitAll()
                    .requestMatchers("/api/v1/public/**")
                    .permitAll()
                    .requestMatchers(
                        "/api/v1/admin/auth/csrf",
                        "/api/v1/admin/auth/login",
                        "/api/v1/admin/auth/logout")
                    .permitAll()
                    .requestMatchers("/api/v1/admin/**")
                    .hasRole("PRINCIPAL")
                    .anyRequest()
                    .denyAll());

    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  AuthenticationEntryPoint authenticationEntryPoint(SecurityProblemResponder responder) {
    return (request, response, exception) -> responder.unauthenticated(request, response);
  }

  @Bean
  AccessDeniedHandler accessDeniedHandler(SecurityProblemResponder responder) {
    return responder::forbidden;
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(properties.allowedOrigins());
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "X-Request-Id"));
    configuration.setExposedHeaders(List.of("X-Request-Id"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
  }
}
