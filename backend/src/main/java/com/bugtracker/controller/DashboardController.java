package com.bugtracker.controller;

import com.bugtracker.model.*;
import com.bugtracker.repository.BugRepository;
import com.bugtracker.repository.ProjectRepository;
import com.bugtracker.repository.UserRepository;
import com.bugtracker.security.BugAccessPolicy;
import com.bugtracker.security.ProjectAccessPolicy;
import com.bugtracker.service.UserService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final BugRepository bugs;
    private final UserRepository users;
    private final ProjectRepository projects;
    private final UserService userService;
    private final ProjectAccessPolicy projectAccess;
    private final BugAccessPolicy bugAccess;

    public DashboardController(BugRepository bugs, UserRepository users, ProjectRepository projects,
            UserService userService, ProjectAccessPolicy projectAccess, BugAccessPolicy bugAccess) {
        this.bugs = bugs;
        this.users = users;
        this.projects = projects;
        this.userService = userService;
        this.projectAccess = projectAccess;
        this.bugAccess = bugAccess;
    }

    @GetMapping("/summary")
    @Transactional(readOnly = true)
    public Map<String, Object> summary(Principal principal) {
        User actor = userService.findEmail(principal.getName());
        List<Project> visibleProjects = projects.findAll().stream()
                .filter(project -> projectAccess.canView(actor, project)).toList();
        List<Bug> visibleBugs = bugs.findAllByOrderByUpdatedAtDesc().stream()
                .filter(bug -> bugAccess.canViewBug(actor, bug)).toList();
        Map<Long, User> participants = visibleBugs.stream().flatMap(b -> java.util.stream.Stream.of(b.getReporter(), b.getAssignee()))
                .filter(Objects::nonNull).collect(java.util.stream.Collectors.toMap(User::getId, user -> user, (first, ignored) -> first));
        long participantCount = participants.size();
        long activeParticipantCount = participants.values().stream().filter(User::isActive).count();
        return Map.of(
                "totalBugs", visibleBugs.size(),
                "open", count(visibleBugs, BugStatus.OPEN),
                "inProgress", count(visibleBugs, BugStatus.IN_PROGRESS),
                "resolved", count(visibleBugs, BugStatus.RESOLVED),
                "closed", count(visibleBugs, BugStatus.CLOSED),
                "reopened", count(visibleBugs, BugStatus.REOPENED),
                "critical", visibleBugs.stream().filter(b -> b.getPriority() == Priority.CRITICAL).count(),
                "users", actor.getRole() == Role.ADMIN ? users.count() : participantCount,
                "activeUsers", actor.getRole() == Role.ADMIN ? users.countByActiveTrue() : activeParticipantCount,
                "projects", visibleProjects.size()
        );
    }

    private long count(List<Bug> bugs, BugStatus status) {
        return bugs.stream().filter(bug -> bug.getStatus() == status).count();
    }
}
