package com.bugtracker.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** A bounded, process-local limiter scoped to a client address and account name. */
@Component
public class LoginAttemptLimiter {
    private final int maxAttempts;
    private final long windowMillis;
    private final int maxTrackedKeys;
    private final Map<String, AttemptWindow> windows = new LinkedHashMap<>(16, 0.75f, true);

    public LoginAttemptLimiter(
            @Value("${app.security.max-login-attempts:10}") int maxAttempts,
            @Value("${app.security.login-window-seconds:60}") long windowSeconds,
            @Value("${app.security.max-tracked-login-identities:10000}") int maxTrackedKeys) {
        if (maxAttempts < 1 || windowSeconds < 1 || maxTrackedKeys < 1) {
            throw new IllegalArgumentException("Login throttling settings must be positive");
        }
        this.maxAttempts = maxAttempts;
        this.windowMillis = Math.multiplyExact(windowSeconds, 1000L);
        this.maxTrackedKeys = maxTrackedKeys;
    }

    public synchronized void recordAttemptAndCheck(String email, String clientAddress) {
        long now = System.currentTimeMillis();
        removeExpired(now);
        String key = key(email, clientAddress);
        AttemptWindow window = windows.get(key);
        if (window != null && now - window.startedAt < windowMillis && window.attempts >= maxAttempts) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many sign-in attempts. Try again shortly.");
        }
        if (window == null || now - window.startedAt >= windowMillis) {
            windows.put(key, new AttemptWindow(now));
        } else {
            window.attempts++;
        }
        while (windows.size() > maxTrackedKeys) {
            Iterator<String> oldest = windows.keySet().iterator();
            oldest.next();
            oldest.remove();
        }
    }

    public synchronized void clear(String email, String clientAddress) {
        windows.remove(key(email, clientAddress));
    }

    private void removeExpired(long now) {
        Iterator<AttemptWindow> iterator = windows.values().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().startedAt >= windowMillis) iterator.remove();
        }
    }

    private String key(String email, String clientAddress) {
        return String.valueOf(clientAddress) + '\u0000' + email;
    }

    private static final class AttemptWindow {
        private final long startedAt;
        private int attempts = 1;

        private AttemptWindow(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}
