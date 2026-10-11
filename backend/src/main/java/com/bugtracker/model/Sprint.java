package com.bugtracker.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name="sprints") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Sprint {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private String name;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="project_id", nullable=false) private Project project;
    private LocalDate startDate;
    private LocalDate endDate;
    @Builder.Default private boolean active=true;
    @Builder.Default private LocalDateTime createdAt=LocalDateTime.now();
}
