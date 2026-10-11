package com.bugtracker.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="audit_logs") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    private String actorEmail;
    private String action;
    private String ipAddress;
    @Column(length=2000) private String details;
    @Builder.Default private LocalDateTime createdAt=LocalDateTime.now();
}
