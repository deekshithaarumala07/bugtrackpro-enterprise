package com.bugtracker.controller;

import com.bugtracker.dto.BugDtos.CreateBugRequest;
import com.bugtracker.dto.BugDtos.UpdateBugRequest;
import com.bugtracker.model.Bug;
import com.bugtracker.model.BugStatus;
import com.bugtracker.model.User;
import com.bugtracker.model.Project;
import com.bugtracker.model.Role;
import com.bugtracker.dto.ApiDtos;
import com.bugtracker.dto.BugDtos.UpdateStatusRequest;
import com.bugtracker.security.BugAccessPolicy;
import com.bugtracker.security.ProjectAccessPolicy;
import com.bugtracker.service.BugService;
import com.bugtracker.service.ProjectService;
import com.bugtracker.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bugs")
public class BugController {

    private final BugService bugService;
    private final UserService userService;
    private final BugAccessPolicy accessPolicy;
    private final ProjectAccessPolicy projectAccess;
    private final ProjectService projects;

    public BugController(
            BugService bugService,
            UserService userService,
            BugAccessPolicy accessPolicy,
            ProjectAccessPolicy projectAccess,
            ProjectService projects
    ) {
        this.bugService = bugService;
        this.userService = userService;
        this.accessPolicy = accessPolicy;
        this.projectAccess = projectAccess;
        this.projects = projects;
    }

    // Get the currently authenticated user.
    private User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required"
            );
        }

        return userService.findEmail(authentication.getName());
    }

    // Get a bug only if the user has permission to view it.
    private Bug accessibleBug(Long id, User user) {
        Bug bug = bugService.find(id);

        if (!accessPolicy.canViewBug(user, bug)) {
            throw new AccessDeniedException(
                    "You do not have permission to view this bug"
            );
        }

        return bug;
    }

    // GET /api/bugs
    // Return only bugs accessible to the authenticated user.
    @GetMapping
    @Transactional(readOnly = true)
    public List<ApiDtos.BugResponse> getAllBugs(Authentication authentication) {
        User user = currentUser(authentication);

        return bugService.all()
                .stream()
                .filter(bug -> accessPolicy.canViewBug(user, bug))
                .map(ApiDtos::bug).toList();
    }

    // GET /api/bugs/{id}
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ApiDtos.BugResponse getBug(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = currentUser(authentication);
        return ApiDtos.bug(accessibleBug(id, user));
    }

    // POST /api/bugs
    // The authenticated user becomes the reporter.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ApiDtos.BugResponse createBug(
            @Valid @RequestBody CreateBugRequest request,
            Authentication authentication
    ) {
        User reporter = currentUser(authentication);
        Project project = projects.find(request.projectId());
        if (!projectAccess.canView(reporter, project)) throw new AccessDeniedException("You do not have access to this project");
        if (request.assigneeId() != null && !projectAccess.canManage(reporter, project))
            throw new AccessDeniedException("Only this project's manager or an admin can assign a bug");
        return ApiDtos.bug(bugService.create(request, reporter));
    }

    // PATCH /api/bugs/{id}/status
    // Example body: {"status":"IN_PROGRESS"}
    @PatchMapping("/{id}/status")
    @Transactional
    public ApiDtos.BugResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request,
            Authentication authentication
    ) {
        User actor = currentUser(authentication);
        Bug bug = bugService.find(id);

        BugStatus status = request.status();
        if (!accessPolicy.canSetStatus(actor, bug, status))
            throw new AccessDeniedException("You do not have permission to set this status");

        return ApiDtos.bug(bugService.status(id, status, actor));
    }

    // PATCH /api/bugs/{id}/assign
    // Example body: {"assigneeId":5}
    // Use {"assigneeId":null} to unassign.
    @PatchMapping("/{id}/assign")
    @Transactional
    public ApiDtos.BugResponse assignBug(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication authentication
    ) {
        User actor = currentUser(authentication);
        Bug bug = bugService.find(id);

        if (!accessPolicy.canAssign(actor, bug)) {
            throw new AccessDeniedException(
                    "You do not have permission to assign this bug"
            );
        }

        Object assigneeValue = request.get("assigneeId");
        Long assigneeId = null;

        if (assigneeValue != null) {
            try {
                if (assigneeValue instanceof Number number) {
                    assigneeId = number.longValue();
                } else {
                    assigneeId = Long.parseLong(
                            assigneeValue.toString()
                    );
                }
            } catch (NumberFormatException exception) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "assigneeId must be a valid user ID"
                );
            }

            if (assigneeId <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "assigneeId must be positive"
                );
            }
        }

        return ApiDtos.bug(bugService.assign(id, assigneeId, actor));
    }

    // PATCH /api/bugs/{id}
    // Update permitted bug metadata.
    @PatchMapping("/{id}")
    @Transactional
    public ApiDtos.BugResponse updateBug(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBugRequest request,
            Authentication authentication
    ) {
        User actor = currentUser(authentication);
        Bug bug = bugService.find(id);

        if (!accessPolicy.canEditMetadata(actor, bug)) {
            throw new AccessDeniedException(
                    "You do not have permission to edit this bug"
            );
        }

        return ApiDtos.bug(bugService.update(id, request, actor));
    }

    // GET /api/bugs/mine
    @GetMapping("/mine")
    @Transactional(readOnly = true)
    public List<ApiDtos.BugResponse> getMyBugs(Authentication authentication) {
        User user = currentUser(authentication);

        return bugService.mine(user)
                .stream()
                .filter(bug -> accessPolicy.canViewBug(user, bug))
                .map(ApiDtos::bug).toList();
    }
}
