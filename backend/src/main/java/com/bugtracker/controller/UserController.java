package com.bugtracker.controller;

import com.bugtracker.dto.ApiDtos;
import com.bugtracker.model.Role;
import com.bugtracker.model.User;
import com.bugtracker.repository.UserRepository;
import com.bugtracker.security.ProjectAccessPolicy;
import com.bugtracker.service.ProjectService;
import com.bugtracker.service.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.security.Principal;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService service;
    private final UserRepository users;
    private final ProjectService projects;
    private final ProjectAccessPolicy projectAccess;

    public UserController(UserService service, UserRepository users, ProjectService projects, ProjectAccessPolicy projectAccess) {
        this.service = service;
        this.users = users;
        this.projects = projects;
        this.projectAccess = projectAccess;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ApiDtos.UserSummary> all() {
        return service.all().stream().map(ApiDtos::user).toList();
    }

    @GetMapping("/developers")
    @Transactional(readOnly = true)
    public List<ApiDtos.UserSummary> developers(@RequestParam Long projectId, Principal principal) {
        User requester = service.findEmail(principal.getName());
        var project = projects.find(projectId);
        if (!projectAccess.canManage(requester, project)) throw new AccessDeniedException("You cannot manage this project");
        return project.getMembers().stream().filter(User::isActive).filter(user -> user.getRole() == Role.DEVELOPER)
                .map(ApiDtos::user).toList();
    }

    @GetMapping("/managers")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ApiDtos.UserSummary> managers() {
        return service.all().stream().filter(User::isActive).filter(user -> user.getRole() == Role.MANAGER)
                .map(ApiDtos::user).toList();
    }

    @GetMapping("/project-candidates")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional(readOnly = true)
    public List<ApiDtos.UserSummary> projectCandidates(@RequestParam Long projectId, Principal principal) {
        User actor = service.findEmail(principal.getName());
        var project = projects.find(projectId);
        if (!projectAccess.canManage(actor, project)) throw new AccessDeniedException("You cannot manage this project");
        return service.all().stream().filter(User::isActive).filter(user -> user.getRole() != Role.ADMIN)
                .map(ApiDtos::user).toList();
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ApiDtos.UserSummary role(@PathVariable Long id, @RequestBody Map<String, String> body, Authentication authentication) {
        User user = service.find(id);
        Role role;
        String requestedRole = body == null ? null : body.get("role");
        try { role = Role.valueOf(requestedRole == null ? "" : requestedRole.trim().toUpperCase()); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("role must be ADMIN, MANAGER, DEVELOPER, or TESTER"); }
        if (user.getEmail().equalsIgnoreCase(authentication.getName()) && role != Role.ADMIN)
            throw new AccessDeniedException("You cannot remove your own administrator role");
        if (user.isActive() && user.getRole() == Role.ADMIN && role != Role.ADMIN && users.countByRoleAndActiveTrue(Role.ADMIN) <= 1)
            throw new IllegalArgumentException("At least one active administrator must remain");
        user.setRole(role);
        return ApiDtos.user(service.save(user));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ApiDtos.UserSummary toggle(@PathVariable Long id, Authentication authentication) {
        User user = service.find(id);
        if (user.getEmail().equalsIgnoreCase(authentication.getName()))
            throw new AccessDeniedException("You cannot deactivate your own account");
        if (user.isActive() && user.getRole() == Role.ADMIN && users.countByRoleAndActiveTrue(Role.ADMIN) <= 1)
            throw new IllegalArgumentException("At least one active administrator must remain");
        user.setActive(!user.isActive());
        return ApiDtos.user(service.save(user));
    }
}
