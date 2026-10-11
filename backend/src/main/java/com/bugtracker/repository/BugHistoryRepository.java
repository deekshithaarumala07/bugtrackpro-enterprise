package com.bugtracker.repository;
import com.bugtracker.model.BugHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface BugHistoryRepository extends JpaRepository<BugHistory,Long> {
 @EntityGraph(attributePaths={"actor"}) List<BugHistory> findByBugIdOrderByCreatedAtDesc(Long bugId);
}
