package com.bugtracker.security;

import com.bugtracker.model.Project;
import com.bugtracker.model.Role;
import com.bugtracker.model.User;
import org.springframework.stereotype.Component;

@Component
public class ProjectAccessPolicy {
    public boolean canManage(User user, Project project) {
        if (user == null || project == null) return false;
        return user.getRole() == Role.ADMIN ||
                (user.getRole() == Role.MANAGER && project.getManager() != null
                        && project.getManager().getId().equals(user.getId()));
    }

    public boolean canView(User user, Project project) {
        if (user == null || project == null) return false;
        if (canManage(user, project)) return true;
        if (project.getMembers() != null && project.getMembers().stream()
                .anyMatch(member -> member.getId().equals(user.getId()))) return true;
        return project.getManager() != null && project.getManager().getId().equals(user.getId());
    }
}
