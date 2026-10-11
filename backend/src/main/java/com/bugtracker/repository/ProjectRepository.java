package com.bugtracker.repository;

import com.bugtracker.model.Project;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    @Override
    @EntityGraph(attributePaths = {"manager", "members"})
    List<Project> findAll();

    @Override
    @EntityGraph(attributePaths = {"manager", "members"})
    Optional<Project> findById(Long id);

    @EntityGraph(attributePaths = {"manager", "members"})
    List<Project> findByActiveTrueOrderByCreatedAtDesc();
}
