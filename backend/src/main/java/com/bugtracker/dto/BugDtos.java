package com.bugtracker.dto;
import com.bugtracker.model.*; import jakarta.validation.constraints.*; import java.time.LocalDateTime;
public class BugDtos {
 public record CreateBugRequest(@NotBlank @Size(max=255) String title,@NotBlank @Size(max=10000) String description,@NotNull @Positive Long projectId,@NotNull Priority priority,@NotNull Severity severity,@Size(max=120) String module,@Size(max=2000) String stepsToReproduce,@Size(max=1000) String environment,@Size(max=120) String category,Long assigneeId,Long sprintId,LocalDateTime dueDate,@PositiveOrZero Double estimatedHours){}
 public record UpdateStatusRequest(@NotNull BugStatus status){}
 public record AssignRequest(Long assigneeId){}
 public record CommentRequest(@NotBlank @Size(max=2000) String content){}
 public record UpdateBugRequest(@Size(max=120) String category,@PositiveOrZero Double actualHours,LocalDateTime dueDate,Long sprintId){}
}
