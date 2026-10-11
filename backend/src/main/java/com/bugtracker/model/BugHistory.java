package com.bugtracker.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="bug_history") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BugHistory {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="bug_id", nullable=false) private Bug bug;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="actor_id") private User actor;
    @Column(nullable=false) private String action;
    @Column(length=2000) private String details;
    @Builder.Default private LocalDateTime createdAt=LocalDateTime.now();
}
