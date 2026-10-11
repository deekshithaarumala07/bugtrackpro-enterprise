package com.bugtracker.repository;
import com.bugtracker.model.Sprint;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SprintRepository extends JpaRepository<Sprint,Long> {
 @EntityGraph(attributePaths={"project","project.manager","project.members"}) List<Sprint> findAllByOrderByStartDateDesc();
 List<Sprint> findByProjectIdOrderByStartDateDesc(Long projectId);
}
