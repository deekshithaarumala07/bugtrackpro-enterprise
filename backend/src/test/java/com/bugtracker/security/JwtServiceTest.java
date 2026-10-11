package com.bugtracker.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    @Test
    void configuredSecretSignsAndValidatesTokens() {
        JwtService service = new JwtService("test-secret-that-is-at-least-32-bytes-long", 60_000, "production");
        String token = service.generate("admin@example.test", "ADMIN", 8L);

        assertEquals("admin@example.test", service.parse(token).getSubject());
        assertEquals("ADMIN", service.parse(token).get("role", String.class));
    }

    @Test
    void developmentUsesTemporaryKeyIfSecretIsUnset() {
        JwtService service = new JwtService("", 60_000, "development");
        assertEquals("developer@example.test", service.parse(service.generate("developer@example.test", "DEVELOPER", 4L)).getSubject());
    }

    @Test
    void productionRejectsMissingOrShortSecret() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService("", 60_000, "production"));
        assertThrows(IllegalArgumentException.class, () -> new JwtService("too-short", 60_000, "production"));
    }
}
