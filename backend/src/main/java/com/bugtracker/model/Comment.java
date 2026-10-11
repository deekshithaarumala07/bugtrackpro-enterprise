package com.bugtracker.model;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime;
@Entity @Table(name="comments") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Comment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,columnDefinition="TEXT") private String content;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="bug_id",nullable=false) private Bug bug;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="author_id",nullable=false) private User author;
 @Builder.Default private LocalDateTime createdAt=LocalDateTime.now();
}
