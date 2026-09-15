package com.rorfost.schoolportal.common.security;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
@EnableJdbcHttpSession(maxInactiveIntervalInSeconds = 28800)
public class JdbcSessionConfiguration {

  @Bean
  CookieSerializer cookieSerializer(
      @Value("${server.servlet.session.cookie.secure:true}") boolean secureCookie) {
    DefaultCookieSerializer serializer = new DefaultCookieSerializer();
    serializer.setUseHttpOnlyCookie(true);
    serializer.setUseSecureCookie(secureCookie);
    serializer.setSameSite("Lax");
    return serializer;
  }

  @Bean
  static BeanPostProcessor jdbcSessionTimeoutConfigurer(
      @Value("${spring.session.timeout:8h}") Duration sessionTimeout) {
    return new BeanPostProcessor() {
      @Override
      public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof JdbcIndexedSessionRepository repository) {
          repository.setDefaultMaxInactiveInterval(sessionTimeout);
        }
        return bean;
      }
    };
  }
}
