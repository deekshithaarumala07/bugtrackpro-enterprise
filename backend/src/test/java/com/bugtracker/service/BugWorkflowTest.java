package com.bugtracker.service;

import com.bugtracker.model.BugStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BugWorkflowTest {
    @Test
    void permitsExpectedLifecycleAndReopenTransitions() {
        assertTrue(BugWorkflow.allows(BugStatus.OPEN, BugStatus.IN_PROGRESS));
        assertTrue(BugWorkflow.allows(BugStatus.IN_PROGRESS, BugStatus.RESOLVED));
        assertTrue(BugWorkflow.allows(BugStatus.RESOLVED, BugStatus.CLOSED));
        assertTrue(BugWorkflow.allows(BugStatus.CLOSED, BugStatus.REOPENED));
        assertTrue(BugWorkflow.allows(BugStatus.REOPENED, BugStatus.IN_PROGRESS));
    }

    @Test
    void rejectsSkippedOrRepeatedTransitions() {
        assertFalse(BugWorkflow.allows(BugStatus.OPEN, BugStatus.CLOSED));
        assertFalse(BugWorkflow.allows(BugStatus.CLOSED, BugStatus.RESOLVED));
        assertFalse(BugWorkflow.allows(BugStatus.IN_PROGRESS, BugStatus.IN_PROGRESS));
        assertFalse(BugWorkflow.allows(null, BugStatus.OPEN));
    }
}
