package com.bugtracker.repository;
import com.bugtracker.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AuditLogRepository extends JpaRepository<AuditLog,Long> {
 List<AuditLog> findTop100ByOrderByCreatedAtDesc();
 long countByAction(String action);
}
