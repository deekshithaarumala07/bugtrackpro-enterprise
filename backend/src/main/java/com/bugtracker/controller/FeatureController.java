package com.bugtracker.controller;

import com.bugtracker.dto.ApiDtos;
import com.bugtracker.model.*;
import com.bugtracker.repository.*;
import com.bugtracker.security.BugAccessPolicy;
import com.bugtracker.security.ProjectAccessPolicy;
import com.bugtracker.service.NotificationService;
import com.bugtracker.service.ProjectService;
import com.bugtracker.service.UserService;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/features")
public class FeatureController {
    private final SprintRepository sprints;
    private final ProjectRepository projects;
    private final BugRepository bugs;
    private final UserService users;
    private final ProjectService projectService;
    private final NotificationService notifications;
    private final AuditLogRepository audits;
    private final ProjectAccessPolicy projectAccess;
    private final BugAccessPolicy bugAccess;

    public FeatureController(SprintRepository sprints, ProjectRepository projects, BugRepository bugs,
            UserService users, ProjectService projectService, NotificationService notifications,
            AuditLogRepository audits, ProjectAccessPolicy projectAccess, BugAccessPolicy bugAccess) {
        this.sprints = sprints;
        this.projects = projects;
        this.bugs = bugs;
        this.users = users;
        this.projectService = projectService;
        this.notifications = notifications;
        this.audits = audits;
        this.projectAccess = projectAccess;
        this.bugAccess = bugAccess;
    }

    private User current(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return users.findEmail(principal.getName());
    }

    private List<Bug> visibleBugs(User user) {
        return bugs.findAllByOrderByUpdatedAtDesc().stream()
                .filter(bug -> bugAccess.canViewBug(user, bug)).toList();
    }

    @GetMapping("/sprints")
    @Transactional(readOnly = true)
    public List<ApiDtos.SprintResponse> sprints(Principal principal) {
        User actor = current(principal);
        return sprints.findAllByOrderByStartDateDesc().stream()
                .filter(sprint -> projectAccess.canView(actor, sprint.getProject()))
                .map(ApiDtos::sprint).toList();
    }

    @PostMapping("/sprints")
    @Transactional
    public ApiDtos.SprintResponse createSprint(@RequestBody Map<String, Object> body, Principal principal) {
        User actor = current(principal);
        Long projectId = longField(body, "projectId", true);
        Project project = projectService.find(projectId);
        if (!projectAccess.canManage(actor, project)) throw new AccessDeniedException("You cannot manage this project");
        String name = stringField(body, "name", true);
        LocalDate start = LocalDate.parse(stringField(body, "startDate", true));
        LocalDate end = LocalDate.parse(stringField(body, "endDate", true));
        if (start.isAfter(end)) throw new IllegalArgumentException("Sprint startDate must be on or before endDate");
        Sprint sprint = Sprint.builder().name(name).project(project).startDate(start).endDate(end).build();
        return ApiDtos.sprint(sprints.save(sprint));
    }

    @PatchMapping("/sprints/{id}/toggle")
    @Transactional
    public ApiDtos.SprintResponse toggleSprint(@PathVariable Long id, Principal principal) {
        User actor = current(principal);
        Sprint sprint = sprints.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sprint not found"));
        if (!projectAccess.canManage(actor, sprint.getProject())) throw new AccessDeniedException("You cannot manage this project");
        sprint.setActive(!sprint.isActive());
        return ApiDtos.sprint(sprints.save(sprint));
    }

    @PatchMapping("/bugs/{id}/sprint")
    @Transactional
    public ApiDtos.BugResponse assignSprint(@PathVariable Long id, @RequestBody Map<String, Object> body, Principal principal) {
        User actor = current(principal);
        Bug bug = bugs.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bug not found"));
        if (!projectAccess.canManage(actor, bug.getProject())) throw new AccessDeniedException("You cannot manage this project");
        Long sprintId = longField(body, "sprintId", false);
        if (sprintId == null) bug.setSprint(null);
        else {
            Sprint sprint = sprints.findById(sprintId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sprint not found"));
            if (!sprint.getProject().getId().equals(bug.getProject().getId())) throw new IllegalArgumentException("Sprint must belong to the bug's project");
            bug.setSprint(sprint);
        }
        bug.setUpdatedAt(LocalDateTime.now());
        return ApiDtos.bug(bugs.save(bug));
    }

    @GetMapping("/notifications")
    public Map<String, Object> notificationList(Principal principal) {
        User actor = current(principal);
        return Map.of("items", notifications.forUser(actor).stream().map(ApiDtos::notification).toList(),
                "unread", notifications.unread(actor));
    }

    @PatchMapping("/notifications/{id}/read")
    public void readNotification(@PathVariable Long id, Principal principal) {
        notifications.markRead(id, current(principal));
    }

    @PatchMapping("/notifications/read-all")
    public void readAll(Principal principal) {
        User actor = current(principal);
        notifications.forUser(actor).forEach(n -> notifications.markRead(n.getId(), actor));
    }

    @GetMapping("/analytics")
    @Transactional(readOnly = true)
    public Map<String, Object> analytics(Principal principal) {
        List<Bug> visible = visibleBugs(current(principal));
        Map<String, Long> byStatus = visible.stream().collect(Collectors.groupingBy(b -> b.getStatus().name(), Collectors.counting()));
        Map<String, Long> byPriority = visible.stream().collect(Collectors.groupingBy(b -> b.getPriority().name(), Collectors.counting()));
        Map<String, Long> bySeverity = visible.stream().collect(Collectors.groupingBy(b -> b.getSeverity().name(), Collectors.counting()));
        Map<String, Long> byDeveloper = visible.stream().filter(b -> b.getAssignee() != null)
                .collect(Collectors.groupingBy(b -> b.getAssignee().getName(), Collectors.counting()));
        long overdue = visible.stream().filter(b -> b.getSlaDueAt() != null && b.getSlaDueAt().isBefore(LocalDateTime.now())
                && b.getStatus() != BugStatus.CLOSED).count();
        double avgResolution = visible.stream().filter(b -> b.getActualHours() != null)
                .mapToDouble(Bug::getActualHours).average().orElse(0);
        return Map.of("byStatus", byStatus, "byPriority", byPriority, "bySeverity", bySeverity,
                "byDeveloper", byDeveloper, "overdue", overdue, "avgResolutionHours", Math.round(avgResolution * 10) / 10.0);
    }

    @GetMapping("/overdue")
    @Transactional(readOnly = true)
    public List<ApiDtos.BugResponse> overdue(Principal principal) {
        return visibleBugs(current(principal)).stream()
                .filter(b -> b.getSlaDueAt() != null && b.getSlaDueAt().isBefore(LocalDateTime.now()) && b.getStatus() != BugStatus.CLOSED)
                .map(ApiDtos::bug).toList();
    }

    @GetMapping("/ai/suggest")
    public Map<String, Object> aiSuggest(@RequestParam String title, @RequestParam(defaultValue = "") String description) {
        String text = (title + " " + description).toLowerCase();
        String module = text.contains("login") || text.contains("password") || text.contains("auth") ? "Authentication"
                : text.contains("payment") || text.contains("checkout") ? "Payments"
                : text.contains("upload") || text.contains("file") ? "File Management"
                : text.contains("database") || text.contains("sql") ? "Database" : "General";
        String severity = text.contains("crash") || text.contains("data loss") || text.contains("security") ? "CRITICAL"
                : text.contains("blocked") || text.contains("cannot") ? "MAJOR" : "MINOR";
        String priority = severity.equals("CRITICAL") ? "CRITICAL" : severity.equals("MAJOR") ? "HIGH" : "MEDIUM";
        String category = text.contains("ui") || text.contains("button") || text.contains("screen") ? "UI" : "Functional";
        return Map.of("module", module, "severity", severity, "priority", priority, "category", category,
                "suggestedSteps", List.of("Open the affected module", "Reproduce the reported action", "Observe the actual result", "Compare with the expected behaviour"),
                "summary", "Heuristic suggestion generated locally; no external AI service is used.");
    }

    @GetMapping("/ai/duplicates")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> duplicates(@RequestParam String title, @RequestParam(defaultValue = "") String description, Principal principal) {
        String input = (title + " " + description).toLowerCase();
        return visibleBugs(current(principal)).stream().map(b -> {
            double score = similarity(input, (b.getTitle() + " " + Objects.toString(b.getDescription(), "")).toLowerCase());
            return Map.<String, Object>of("bug", ApiDtos.bug(b), "similarity", Math.round(score * 100));
        }).filter(x -> ((Long) x.get("similarity")) >= 45)
                .sorted((a, b) -> Long.compare((Long) b.get("similarity"), (Long) a.get("similarity"))).limit(5).toList();
    }

    private double similarity(String a, String b) {
        Set<String> x = words(a), y = words(b);
        if (x.isEmpty() || y.isEmpty()) return 0;
        Set<String> intersection = new HashSet<>(x); intersection.retainAll(y);
        Set<String> union = new HashSet<>(x); union.addAll(y);
        return (double) intersection.size() / union.size();
    }
    private Set<String> words(String value) {
        return Arrays.stream(value.replaceAll("[^a-z0-9 ]", " ").split("\\s+"))
                .filter(w -> w.length() > 2).collect(Collectors.toSet());
    }

    @GetMapping(value = "/reports/bugs.csv", produces = "text/csv")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> csv(Principal principal) {
        StringBuilder out = new StringBuilder("ID,Title,Project,Status,Priority,Severity,Assignee,Created,Updated,SLA Due\n");
        for (Bug b : visibleBugs(current(principal))) {
            out.append(csvValue(b.getId())).append(',').append(csvValue(b.getTitle())).append(',')
                    .append(csvValue(b.getProject().getName())).append(',').append(b.getStatus()).append(',')
                    .append(b.getPriority()).append(',').append(b.getSeverity()).append(',')
                    .append(csvValue(b.getAssignee() == null ? "" : b.getAssignee().getName())).append(',')
                    .append(csvValue(b.getCreatedAt())).append(',').append(csvValue(b.getUpdatedAt())).append(',')
                    .append(csvValue(b.getSlaDueAt())).append('\n');
        }
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bug-report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(out.toString().getBytes(StandardCharsets.UTF_8));
    }
    private String csvValue(Object value) {
        String text = String.valueOf(value == null ? "" : value);
        if (startsWithSpreadsheetFormula(text)) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private boolean startsWithSpreadsheetFormula(String text) {
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (Character.isISOControl(character)) return true;
            if (Character.isWhitespace(character) || Character.isSpaceChar(character) || character == '\uFEFF') continue;
            return character == '=' || character == '+' || character == '-' || character == '@';
        }
        return false;
    }

    @GetMapping("/security")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> security() {
        return Map.of("auditEvents", audits.count(), "failedLogins", audits.countByAction("LOGIN_FAILED"),
                "successfulLogins", audits.countByAction("LOGIN_SUCCESS"), "securityNotes",
                List.of("JWT authentication", "BCrypt password hashing", "Role-based endpoint authorization", "Login rate limiting", "Attachment validation", "Audit logging"),
                "recentAudit", audits.findTop100ByOrderByCreatedAtDesc().stream().limit(20)
                        .map(a -> Map.of("action", a.getAction(), "actor", Objects.toString(a.getActorEmail(), ""),
                                "ip", Objects.toString(a.getIpAddress(), ""), "details", Objects.toString(a.getDetails(), ""),
                                "createdAt", a.getCreatedAt())).toList());
    }

    private Long longField(Map<String, Object> body, String field, boolean required) {
        Object value = body == null ? null : body.get(field);
        if (value == null) {
            if (required) throw new IllegalArgumentException(field + " is required");
            return null;
        }
        try { return value instanceof Number n ? n.longValue() : Long.valueOf(value.toString()); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException(field + " must be an integer"); }
    }
    private String stringField(Map<String, Object> body, String field, boolean required) {
        Object value = body == null ? null : body.get(field);
        String text = value == null ? "" : value.toString().trim();
        if (required && text.isBlank()) throw new IllegalArgumentException(field + " is required");
        return text;
    }
}
