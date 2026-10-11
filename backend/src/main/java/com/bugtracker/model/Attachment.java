package com.bugtracker.model;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime;
@Entity @Table(name="attachments") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Attachment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private String originalName;
 @Column(nullable=false) private String storedName;
 private String contentType; private long size;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="bug_id",nullable=false) private Bug bug;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="uploaded_by",nullable=false) private User uploadedBy;
 @Builder.Default private LocalDateTime uploadedAt=LocalDateTime.now();
}
