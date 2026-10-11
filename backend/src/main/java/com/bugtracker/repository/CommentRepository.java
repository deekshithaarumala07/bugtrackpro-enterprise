package com.bugtracker.repository;

import com.bugtracker.model.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = {"author"})
    List<Comment> findByBugIdOrderByCreatedAtAsc(Long bugId);
}
