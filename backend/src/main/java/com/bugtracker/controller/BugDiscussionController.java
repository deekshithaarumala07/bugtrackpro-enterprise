package com.bugtracker.controller;

import com.bugtracker.dto.ApiDtos;
import com.bugtracker.dto.BugDtos.CommentRequest;
import com.bugtracker.model.Bug;
import com.bugtracker.model.Comment;
import com.bugtracker.model.User;
import com.bugtracker.repository.BugHistoryRepository;
import com.bugtracker.repository.CommentRepository;
import com.bugtracker.security.BugAccessPolicy;
import com.bugtracker.security.ProjectAccessPolicy;
import com.bugtracker.service.BugService;
import com.bugtracker.service.HistoryService;
import com.bugtracker.service.NotificationService;
import com.bugtracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class BugDiscussionController {
    private final BugService bugs;
    private final UserService users;
    private final CommentRepository comments;
    private final BugHistoryRepository history;
    private final HistoryService historyService;
    private final NotificationService notifications;
    private final BugAccessPolicy bugAccess;
    private final ProjectAccessPolicy projectAccess;

    public BugDiscussionController(BugService bugs, UserService users, CommentRepository comments,
            BugHistoryRepository history, HistoryService historyService, NotificationService notifications,
            BugAccessPolicy bugAccess, ProjectAccessPolicy projectAccess) {
        this.bugs = bugs; this.users = users; this.comments = comments; this.history = history;
        this.historyService = historyService; this.notifications = notifications;
        this.bugAccess = bugAccess; this.projectAccess = projectAccess;
    }

    private User actor(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return users.findEmail(authentication.getName());
    }

    private Bug accessible(Long id, User user) {
        Bug bug = bugs.find(id);
        if (!bugAccess.canViewBug(user, bug)) throw new AccessDeniedException("You do not have access to this issue");
        return bug;
    }

    @GetMapping("/bugs/{bugId}/comments")
    @Transactional(readOnly = true)
    public List<ApiDtos.CommentResponse> comments(@PathVariable Long bugId, Authentication authentication) {
        accessible(bugId, actor(authentication));
        return comments.findByBugIdOrderByCreatedAtAsc(bugId).stream().map(ApiDtos::comment).toList();
    }

    @PostMapping("/bugs/{bugId}/comments")
    @Transactional
    public ApiDtos.CommentResponse comment(@PathVariable Long bugId, @Valid @RequestBody CommentRequest request,
            Authentication authentication) {
        User author = actor(authentication);
        Bug bug = accessible(bugId, author);
        Comment saved = comments.save(Comment.builder().bug(bug).author(author).content(request.content().trim()).build());
        historyService.add(bug, author, "COMMENT_ADDED", "Comment added");
        if (bug.getReporter() != null && !bug.getReporter().getId().equals(author.getId()))
            notifications.create(bug.getReporter(), "Issue comment", "A comment was added to #" + bug.getId(), "COMMENT", "/bugs/" + bug.getId());
        if (bug.getAssignee() != null && !bug.getAssignee().getId().equals(author.getId())
                && (bug.getReporter() == null || !bug.getReporter().getId().equals(bug.getAssignee().getId())))
            notifications.create(bug.getAssignee(), "Issue comment", "A comment was added to #" + bug.getId(), "COMMENT", "/bugs/" + bug.getId());
        return ApiDtos.comment(saved);
    }

    @PatchMapping("/comments/{id}")
    @Transactional
    public ApiDtos.CommentResponse editComment(@PathVariable Long id, @Valid @RequestBody CommentRequest request,
            Authentication authentication) {
        User editor = actor(authentication);
        Comment comment = comments.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
        requireIssueAccess(editor, comment);
        if (!comment.getAuthor().getId().equals(editor.getId()) && !projectAccess.canManage(editor, comment.getBug().getProject()))
            throw new AccessDeniedException("Only the author or project manager can edit this comment");
        comment.setContent(request.content().trim());
        historyService.add(comment.getBug(), editor, "COMMENT_EDITED", "Comment " + id + " edited");
        return ApiDtos.comment(comments.save(comment));
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteComment(@PathVariable Long id, Authentication authentication) {
        User editor = actor(authentication);
        Comment comment = comments.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
        requireIssueAccess(editor, comment);
        if (!comment.getAuthor().getId().equals(editor.getId()) && !projectAccess.canManage(editor, comment.getBug().getProject()))
            throw new AccessDeniedException("Only the author or project manager can delete this comment");
        historyService.add(comment.getBug(), editor, "COMMENT_DELETED", "Comment " + id + " deleted");
        comments.delete(comment);
    }

    private void requireIssueAccess(User user, Comment comment) {
        if (!bugAccess.canViewBug(user, comment.getBug()))
            throw new AccessDeniedException("You do not have access to this issue");
    }

    @GetMapping("/bugs/{bugId}/history")
    @Transactional(readOnly = true)
    public List<ApiDtos.HistoryResponse> history(@PathVariable Long bugId, Authentication authentication) {
        accessible(bugId, actor(authentication));
        return history.findByBugIdOrderByCreatedAtDesc(bugId).stream().map(ApiDtos::history).toList();
    }
}
