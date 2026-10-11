package com.bugtracker.repository;

import com.bugtracker.model.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BugRepository extends JpaRepository<Bug, Long> {
    long countByStatus(BugStatus status);
    long countByPriority(Priority priority);
    long countByAssigneeId(Long id);

    @EntityGraph(attributePaths = {"project", "project.manager", "project.members", "reporter", "assignee", "sprint", "sprint.project", "sprint.project.manager"})
    List<Bug> findAllByOrderByUpdatedAtDesc();

    @EntityGraph(attributePaths = {"project", "project.manager", "project.members", "reporter", "assignee", "sprint", "sprint.project", "sprint.project.manager"})
    List<Bug> findByReporterIdOrAssigneeIdOrderByUpdatedAtDesc(Long reporterId, Long assigneeId);

    @EntityGraph(attributePaths = {"project", "project.manager", "project.members", "reporter", "assignee", "sprint", "sprint.project", "sprint.project.manager"})
    List<Bug> findByProjectIdOrderByUpdatedAtDesc(Long projectId);

    @Override
    @EntityGraph(attributePaths = {"project", "project.manager", "project.members", "reporter", "assignee", "sprint", "sprint.project", "sprint.project.manager"})
    java.util.Optional<Bug> findById(Long id);
}
