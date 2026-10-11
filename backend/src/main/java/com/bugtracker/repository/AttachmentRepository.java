package com.bugtracker.repository;

import com.bugtracker.model.Attachment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    @EntityGraph(attributePaths = {"uploadedBy"})
    List<Attachment> findByBugIdOrderByUploadedAtDesc(Long bugId);
}
