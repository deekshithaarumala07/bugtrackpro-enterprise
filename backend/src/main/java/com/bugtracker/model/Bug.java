package com.bugtracker.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="bugs") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Bug {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(nullable=false) private String title;
  @Column(nullable=false, columnDefinition="TEXT") private String description;
  private String module;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private Priority priority;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private Severity severity;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private BugStatus status;
  @Column(length=2000) private String stepsToReproduce;
  @Column(length=1000) private String environment;
  private String category;
  private LocalDateTime dueDate;
  private LocalDateTime slaDueAt;
  private Double estimatedHours;
  private Double actualHours;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="project_id", nullable=false) private Project project;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="reporter_id", nullable=false) private User reporter;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="assignee_id") private User assignee;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="sprint_id") private Sprint sprint;
  @Builder.Default private LocalDateTime createdAt=LocalDateTime.now();
  @Builder.Default private LocalDateTime updatedAt=LocalDateTime.now();
}
