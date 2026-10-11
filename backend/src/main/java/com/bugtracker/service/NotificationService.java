package com.bugtracker.service;

import com.bugtracker.model.Notification;
import com.bugtracker.model.User;
import com.bugtracker.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NotificationService {
 private final NotificationRepository repo;
 public NotificationService(NotificationRepository repo){this.repo=repo;}
 public Notification create(User user,String title,String message,String type,String link){if(user==null)return null;return repo.save(Notification.builder().user(user).title(title).message(message).type(type).link(link).build());}
 public List<Notification> forUser(User u){return repo.findTop30ByUserIdOrderByCreatedAtDesc(u.getId());}
 public long unread(User u){return repo.countByUserIdAndReadFalse(u.getId());}
 public void markRead(Long id,User u){repo.findById(id).ifPresent(n->{if(n.getUser().getId().equals(u.getId())){n.setRead(true);repo.save(n);}});}
}
