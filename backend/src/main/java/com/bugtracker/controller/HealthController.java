package com.bugtracker.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class HealthController {
    private static final Logger log = LoggerFactory.getLogger(HealthController.class);
    private final DataSource dataSource;

    public HealthController(DataSource dataSource) { this.dataSource = dataSource; }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> health() {
        try (var connection = dataSource.getConnection()) {
            if (connection.isValid(2))
                return ResponseEntity.ok(Map.of("status", "UP", "service", "Bug Tracking System API", "database", "UP"));
        } catch (SQLException ex) {
            log.warn("Database health probe failed (details withheld from public response)");
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("status", "DOWN", "service", "Bug Tracking System API", "database", "DOWN"));
    }
}
