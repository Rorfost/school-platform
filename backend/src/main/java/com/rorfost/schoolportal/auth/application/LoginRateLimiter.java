package com.rorfost.schoolportal.auth.application;

import com.rorfost.schoolportal.common.config.SecurityProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiter {

  private final ConcurrentHashMap<String, Deque<Instant>> failedAttempts =
      new ConcurrentHashMap<>();
  private final SecurityProperties properties;
  private final Clock clock;

  @Autowired
  public LoginRateLimiter(SecurityProperties properties) {
    this(properties, Clock.systemUTC());
  }

  LoginRateLimiter(SecurityProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
  }

  public void checkAllowed(String key) {
    Deque<Instant> attempts = failedAttempts.get(key);
    if (attempts == null) {
      return;
    }
    synchronized (attempts) {
      removeExpired(attempts);
      if (attempts.size() >= properties.loginMaxAttempts()) {
        throw new LoginRateLimitExceededException();
      }
      if (attempts.isEmpty()) {
        failedAttempts.remove(key, attempts);
      }
    }
  }

  public void recordFailure(String key) {
    Deque<Instant> attempts = failedAttempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
    synchronized (attempts) {
      removeExpired(attempts);
      attempts.addLast(clock.instant());
    }
  }

  public void clear(String key) {
    failedAttempts.remove(key);
  }

  private void removeExpired(Deque<Instant> attempts) {
    Instant threshold = clock.instant().minus(properties.loginAttemptWindow());
    while (!attempts.isEmpty() && !attempts.peekFirst().isAfter(threshold)) {
      attempts.removeFirst();
    }
  }
}
