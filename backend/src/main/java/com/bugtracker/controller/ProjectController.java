package com.bugtracker.controller;

import com.bugtracker.dto.ApiDtos;
import com.bugtracker.dto.ApiDtos.ProjectResponse;
import com.bugtracker.dto.ProjectDtos.CreateProjectRequest;
import com.bugtracker.service.ProjectService;
import com.bugtracker.service.UserService;
import com.bugtracker.model.Project;
import com.bugtracker.model.User;
import com.bugtracker.model.Role;
import com.bugtracker.security.ProjectAccessPolicy;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService service;
    private final UserService users;
    private final ProjectAccessPolicy access;

    public ProjectController(ProjectService service, UserService users, ProjectAccessPolicy access) {
        this.service = service;
        this.users = users;
        this.access = access;
    }

    private User current(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return users.findEmail(auth.getName());
    }

    private Project visible(Long id, User user) {
        Project project = service.find(id);
        if (!access.canView(user, project)) throw new AccessDeniedException("You do not have access to this project");
        return project;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<ProjectResponse> all(Authentication authentication) {
        User user = current(authentication);
        return service.all().stream().filter(p -> access.canView(user, p)).map(ApiDtos::project).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ProjectResponse one(@PathVariable Long id, Authentication authentication) {
        return ApiDtos.project(visible(id, current(authentication)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Transactional
    public ProjectResponse create(@Valid @RequestBody CreateProjectRequest request, Authentication authentication) {
        User actor = current(authentication);
        if (actor.getRole() != Role.ADMIN && actor.getRole() != Role.MANAGER)
            throw new AccessDeniedException("Only admins and managers can create projects");
        return ApiDtos.project(service.create(request, actor));
    }

    @PostMapping("/{id}/members/{userId}")
    @Transactional
    public ProjectResponse addMember(@PathVariable Long id, @PathVariable Long userId, Authentication authentication) {
        User actor = current(authentication);
        Project project = service.find(id);
        if (!access.canManage(actor, project)) throw new AccessDeniedException("Only the project manager or an admin can change membership");
        return ApiDtos.project(service.addMember(id, userId));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Transactional
    public ProjectResponse removeMember(@PathVariable Long id, @PathVariable Long userId, Authentication authentication) {
        User actor = current(authentication);
        Project project = service.find(id);
        if (!access.canManage(actor, project)) throw new AccessDeniedException("Only the project manager or an admin can change membership");
        return ApiDtos.project(service.removeMember(id, userId));
    }
}
