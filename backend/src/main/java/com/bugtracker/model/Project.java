package com.bugtracker.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity @Table(name="projects") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Project {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(nullable=false, unique=true) private String name;
  @Column(length=1000) private String description;
  @Builder.Default private boolean active=true;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="manager_id") private User manager;
  @ManyToMany(fetch=FetchType.LAZY)
  @JoinTable(name="project_members",
      joinColumns=@JoinColumn(name="project_id"),
      inverseJoinColumns=@JoinColumn(name="user_id"))
  @Builder.Default private Set<User> members=new HashSet<>();
  @Builder.Default private LocalDateTime createdAt=LocalDateTime.now();
}
