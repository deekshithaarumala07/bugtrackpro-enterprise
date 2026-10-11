package com.bugtracker.repository;
import com.bugtracker.model.Notification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface NotificationRepository extends JpaRepository<Notification,Long> {
 @EntityGraph(attributePaths={"user"}) List<Notification> findTop30ByUserIdOrderByCreatedAtDesc(Long userId);
 long countByUserIdAndReadFalse(Long userId);
}
