package com.bugtracker.service;
import com.bugtracker.model.*; import com.bugtracker.repository.BugHistoryRepository; import org.springframework.stereotype.Service;
@Service public class HistoryService { private final BugHistoryRepository repo; public HistoryService(BugHistoryRepository repo){this.repo=repo;} public BugHistory add(Bug bug,User actor,String action,String details){return repo.save(BugHistory.builder().bug(bug).actor(actor).action(action).details(details).build());} }
