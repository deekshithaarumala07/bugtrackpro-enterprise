package com.bugtracker.service;

import com.bugtracker.dto.ProjectDtos.CreateProjectRequest;
import com.bugtracker.model.Project;
import com.bugtracker.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import com.bugtracker.model.Role;
import com.bugtracker.model.User;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {
    private final ProjectRepository repo;
    private final UserService users;

    public ProjectService(ProjectRepository repo, UserService users) {
        this.repo = repo;
        this.users = users;
    }

    public List<Project> all() {
        return repo.findAll();
    }

    public Project find(Long id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Project not found"));
    }

    public Project create(CreateProjectRequest request, User actor) {
        Project project = Project.builder()
                .name(request.name())
                .description(request.description())
                .build();

        if (actor.getRole() == Role.MANAGER) {
            project.setManager(actor);
        } else if (request.managerId() != null) {
            User manager = users.find(request.managerId());
            if (!manager.isActive() || manager.getRole() != Role.MANAGER)
                throw new IllegalArgumentException("Project owner must be an active manager");
            project.setManager(manager);
        }

        return repo.save(project);
    }

    @Transactional
    public Project addMember(Long projectId, Long userId) {
        Project project = find(projectId);
        User user = users.find(userId);
        if (!user.isActive()) throw new IllegalArgumentException("Inactive users cannot be added to a project");
        project.getMembers().add(user);
        return repo.save(project);
    }

    @Transactional
    public Project removeMember(Long projectId, Long userId) {
        Project project = find(projectId);
        project.getMembers().removeIf(member -> member.getId().equals(userId));
        return repo.save(project);
    }
}
