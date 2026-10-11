package com.bugtracker.service;

import com.bugtracker.model.BugStatus;

public final class BugWorkflow {
    private BugWorkflow() {}

    public static boolean allows(BugStatus from, BugStatus to) {
        if (from == null || to == null) return false;
        return switch (from) {
            case OPEN -> to == BugStatus.IN_PROGRESS || to == BugStatus.REOPENED;
            case IN_PROGRESS -> to == BugStatus.OPEN || to == BugStatus.RESOLVED;
            case RESOLVED -> to == BugStatus.IN_PROGRESS || to == BugStatus.CLOSED || to == BugStatus.REOPENED;
            case CLOSED -> to == BugStatus.REOPENED;
            case REOPENED -> to == BugStatus.IN_PROGRESS || to == BugStatus.RESOLVED;
        };
    }
}
