
package com.bugtracker.security;

import com.bugtracker.model.Bug;
import com.bugtracker.model.User;
import com.bugtracker.model.Project;
import org.springframework.stereotype.Component;

@Component
public class BugAccessPolicy {
    private final ProjectAccessPolicy projects;

    public BugAccessPolicy(ProjectAccessPolicy projects) { this.projects = projects; }

    public boolean isAdmin(User user) {
        return user != null && user.getRole() != null
                && user.getRole().name().equals("ADMIN");
    }

    public boolean canManageProject(User user, Bug bug) {
        if (user == null || bug == null) {
            return false;
        }

        if (isAdmin(user)) {
            return true;
        }

        return projects.canManage(user, bug.getProject());
    }

    public boolean isAssignee(User user, Bug bug) {
        return user != null && bug != null
                && bug.getAssignee() != null
                && bug.getAssignee().getId().equals(user.getId());
    }

    public boolean isReporter(User user, Bug bug) {
        return user != null && bug != null
                && bug.getReporter() != null
                && bug.getReporter().getId().equals(user.getId());
    }

    public boolean canViewBug(User user, Bug bug) {
        if (user == null || bug == null) {
            return false;
        }

        return projects.canView(user, bug.getProject())
                || isAssignee(user, bug)
                || isReporter(user, bug);
    }

    public boolean canChangeStatus(User user, Bug bug) {
        if (user == null || bug == null || user.getRole() == null) {
            return false;
        }

        if (isAdmin(user) || canManageProject(user, bug)) {
            return true;
        }

        String role = user.getRole().name();

        if ("DEVELOPER".equals(role)) {
            return isAssignee(user, bug);
        }

        if ("TESTER".equals(role)) {
            return isReporter(user, bug) || isAssignee(user, bug);
        }

        return false;
    }

    public boolean canSetStatus(User user, Bug bug, com.bugtracker.model.BugStatus target) {
        if (!canChangeStatus(user, bug) || target == null) return false;
        return switch (user.getRole()) {
            case ADMIN, MANAGER -> true;
            case DEVELOPER -> target == com.bugtracker.model.BugStatus.IN_PROGRESS
                    || target == com.bugtracker.model.BugStatus.RESOLVED;
            case TESTER -> isReporter(user, bug)
                    && target == com.bugtracker.model.BugStatus.REOPENED;
        };
    }

    public boolean canAssign(User user, Bug bug) {
        return canManageProject(user, bug);
    }

    public boolean canEditMetadata(User user, Bug bug) {
        if (user == null || bug == null || user.getRole() == null) {
            return false;
        }

        return isAdmin(user)
                || canManageProject(user, bug)
                || ("DEVELOPER".equals(user.getRole().name())
                    && isAssignee(user, bug));
    }
}
