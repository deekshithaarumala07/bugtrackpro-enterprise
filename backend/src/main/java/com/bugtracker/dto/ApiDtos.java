package com.bugtracker.dto;

import com.bugtracker.model.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class ApiDtos {
    private ApiDtos() {}
    public record UserSummary(Long id,String name,String email,Role role,boolean active,LocalDateTime createdAt) {}
    public record ProjectResponse(Long id,String name,String description,boolean active,UserSummary manager,java.util.List<UserSummary> members,LocalDateTime createdAt) {}
    public record SprintResponse(Long id,String name,Long projectId,String projectName,LocalDate startDate,LocalDate endDate,boolean active) {}
    public record BugResponse(Long id,String title,String description,String module,Priority priority,Severity severity,BugStatus status,
                              String stepsToReproduce,String environment,String category,LocalDateTime dueDate,LocalDateTime slaDueAt,
                              Double estimatedHours,Double actualHours,ProjectResponse project,UserSummary reporter,UserSummary assignee,
                              SprintResponse sprint,LocalDateTime createdAt,LocalDateTime updatedAt) {}
    public record CommentResponse(Long id,String content,UserSummary author,LocalDateTime createdAt) {}
    public record AttachmentResponse(Long id,String originalName,String contentType,long size,UserSummary uploadedBy,LocalDateTime uploadedAt) {}
    public record HistoryResponse(Long id,String action,String details,UserSummary actor,LocalDateTime createdAt) {}
    public record NotificationResponse(Long id,String title,String message,String type,String link,boolean read,LocalDateTime createdAt) {}
    public static UserSummary user(User u){if(u==null)return null;return new UserSummary(u.getId(),u.getName(),u.getEmail(),u.getRole(),u.isActive(),u.getCreatedAt());}
    public static ProjectResponse project(Project p){if(p==null)return null;return new ProjectResponse(p.getId(),p.getName(),p.getDescription(),p.isActive(),user(p.getManager()),p.getMembers()==null?java.util.List.of():p.getMembers().stream().map(ApiDtos::user).toList(),p.getCreatedAt());}
    public static SprintResponse sprint(Sprint s){if(s==null)return null;return new SprintResponse(s.getId(),s.getName(),s.getProject().getId(),s.getProject().getName(),s.getStartDate(),s.getEndDate(),s.isActive());}
    public static BugResponse bug(Bug b){if(b==null)return null;return new BugResponse(b.getId(),b.getTitle(),b.getDescription(),b.getModule(),b.getPriority(),b.getSeverity(),b.getStatus(),b.getStepsToReproduce(),b.getEnvironment(),b.getCategory(),b.getDueDate(),b.getSlaDueAt(),b.getEstimatedHours(),b.getActualHours(),project(b.getProject()),user(b.getReporter()),user(b.getAssignee()),sprint(b.getSprint()),b.getCreatedAt(),b.getUpdatedAt());}
    public static CommentResponse comment(Comment c){return new CommentResponse(c.getId(),c.getContent(),user(c.getAuthor()),c.getCreatedAt());}
    public static AttachmentResponse attachment(Attachment a){return new AttachmentResponse(a.getId(),a.getOriginalName(),a.getContentType(),a.getSize(),user(a.getUploadedBy()),a.getUploadedAt());}
    public static HistoryResponse history(BugHistory h){return new HistoryResponse(h.getId(),h.getAction(),h.getDetails(),user(h.getActor()),h.getCreatedAt());}
    public static NotificationResponse notification(Notification n){return new NotificationResponse(n.getId(),n.getTitle(),n.getMessage(),n.getType(),n.getLink(),n.isRead(),n.getCreatedAt());}
}
