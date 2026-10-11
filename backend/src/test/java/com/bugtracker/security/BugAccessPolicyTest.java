package com.bugtracker.security;

import com.bugtracker.model.*;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class BugAccessPolicyTest {
    private final BugAccessPolicy policy = new BugAccessPolicy(new ProjectAccessPolicy());

    private User user(long id, Role role) {
        return User.builder().id(id).name("User " + id).email("user" + id + "@example.test")
                .role(role).active(true).build();
    }

    private Bug bug(Project project, User reporter, User assignee) {
        return Bug.builder().id(70L).project(project).reporter(reporter).assignee(assignee)
                .status(BugStatus.OPEN).build();
    }

    @Test
    void blocksUnrelatedUserFromPrivateProjectBug() {
        User manager = user(1, Role.MANAGER);
        User reporter = user(2, Role.TESTER);
        Project project = Project.builder().id(5L).manager(manager).members(Set.of(reporter)).build();
        Bug bug = bug(project, reporter, user(3, Role.DEVELOPER));

        assertFalse(policy.canViewBug(user(4, Role.TESTER), bug));
        assertTrue(policy.canViewBug(reporter, bug));
    }

    @Test
    void developerMayWorkAssignedBugButCannotCloseIt() {
        User manager = user(1, Role.MANAGER);
        User developer = user(3, Role.DEVELOPER);
        Project project = Project.builder().id(5L).manager(manager).build();
        Bug bug = bug(project, user(2, Role.TESTER), developer);

        assertTrue(policy.canSetStatus(developer, bug, BugStatus.IN_PROGRESS));
        assertTrue(policy.canSetStatus(developer, bug, BugStatus.RESOLVED));
        assertFalse(policy.canSetStatus(developer, bug, BugStatus.CLOSED));
    }

    @Test
    void testerMayReopenReportedBugButCannotCloseIt() {
        User tester = user(2, Role.TESTER);
        Project project = Project.builder().id(5L).manager(user(1, Role.MANAGER)).build();
        Bug bug = bug(project, tester, user(3, Role.DEVELOPER));

        assertTrue(policy.canSetStatus(tester, bug, BugStatus.REOPENED));
        assertFalse(policy.canSetStatus(tester, bug, BugStatus.CLOSED));
    }

    @Test
    void owningManagerCanManageProjectIssues() {
        User manager = user(1, Role.MANAGER);
        Project project = Project.builder().id(5L).manager(manager).build();
        Bug bug = bug(project, user(2, Role.TESTER), user(3, Role.DEVELOPER));

        assertTrue(policy.canViewBug(manager, bug));
        assertTrue(policy.canAssign(manager, bug));
    }
}
