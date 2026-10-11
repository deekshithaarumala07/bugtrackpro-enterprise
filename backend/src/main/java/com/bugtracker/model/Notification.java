package com.bugtracker.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="notifications") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id", nullable=false) private User user;
    @Column(nullable=false) private String title;
    @Column(length=2000, nullable=false) private String message;
    private String type;
    private String link;
    @Builder.Default private boolean read=false;
    @Builder.Default private LocalDateTime createdAt=LocalDateTime.now();
}
