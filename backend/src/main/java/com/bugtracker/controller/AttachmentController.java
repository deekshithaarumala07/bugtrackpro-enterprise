package com.bugtracker.controller;

import com.bugtracker.dto.ApiDtos;
import com.bugtracker.model.Attachment;
import com.bugtracker.model.Bug;
import com.bugtracker.model.User;
import com.bugtracker.repository.AttachmentRepository;
import com.bugtracker.security.BugAccessPolicy;
import com.bugtracker.service.BugService;
import com.bugtracker.service.HistoryService;
import com.bugtracker.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/bugs")
public class AttachmentController {
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private final AttachmentRepository attachments;
    private final BugService bugs;
    private final UserService users;
    private final BugAccessPolicy access;
    private final HistoryService history;
    private final Path root;

    public AttachmentController(AttachmentRepository attachments, BugService bugs, UserService users,
            BugAccessPolicy access, HistoryService history, @Value("${app.upload-dir:uploads}") String uploadDir) {
        this.attachments = attachments; this.bugs = bugs; this.users = users; this.access = access;
        this.history = history; this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private User actor(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return users.findEmail(auth.getName());
    }
    private Bug accessible(Long bugId, User user) {
        Bug bug = bugs.find(bugId);
        if (!access.canViewBug(user, bug)) throw new AccessDeniedException("You do not have access to this issue");
        return bug;
    }

    @GetMapping("/{bugId}/attachments")
    @Transactional(readOnly = true)
    public List<ApiDtos.AttachmentResponse> list(@PathVariable Long bugId, Authentication auth) {
        accessible(bugId, actor(auth));
        return attachments.findByBugIdOrderByUploadedAtDesc(bugId).stream().map(ApiDtos::attachment).toList();
    }

    @PostMapping(value = "/{bugId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ApiDtos.AttachmentResponse upload(@PathVariable Long bugId, @RequestParam("file") MultipartFile file,
            Authentication auth) {
        User uploader = actor(auth);
        Bug bug = accessible(bugId, uploader);
        if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a non-empty file");
        if (file.getSize() > MAX_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds the 10 MB limit");
        String original = safeOriginalName(file.getOriginalFilename());
        String extension = extension(original);
        byte[] content;
        try { content = file.getBytes(); }
        catch (IOException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded file could not be read"); }
        if (!validContent(extension, content)) throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only valid PNG, JPEG, GIF, PDF, TXT, CSV, and LOG files are allowed");

        String storedName = UUID.randomUUID() + extension;
        Path destination = root.resolve(storedName).normalize();
        if (!destination.startsWith(root)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path");
        try {
            Files.createDirectories(root);
            Files.write(destination, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            Attachment saved = attachments.save(Attachment.builder().bug(bug).uploadedBy(uploader).originalName(original)
                    .storedName(storedName).contentType(file.getContentType()).size(content.length).build());
            history.add(bug, uploader, "ATTACHMENT_ADDED", "Attachment added: " + original);
            return ApiDtos.attachment(saved);
        } catch (IOException | RuntimeException ex) {
            try { Files.deleteIfExists(destination); } catch (IOException cleanup) { ex.addSuppressed(cleanup); }
            if (ex instanceof ResponseStatusException response) throw response;
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Attachment could not be saved");
        }
    }

    @GetMapping("/attachments/{attachmentId}/download")
    @Transactional(readOnly = true)
    public ResponseEntity<ByteArrayResource> download(@PathVariable Long attachmentId, Authentication auth) {
        User requester = actor(auth);
        Attachment attachment = attachments.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));
        accessible(attachment.getBug().getId(), requester);
        Path path = root.resolve(attachment.getStoredName()).normalize();
        if (!path.startsWith(root)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found");
        try {
            byte[] data = Files.readAllBytes(path);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(data.length).header("X-Content-Type-Options", "nosniff")
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(attachment.getOriginalName(), StandardCharsets.UTF_8).build().toString())
                    .body(new ByteArrayResource(data));
        } catch (NoSuchFileException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment file is missing");
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Attachment could not be read");
        }
    }

    private String safeOriginalName(String name) {
        if (name == null || name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A filename is required");
        String normalized = name.replace('\\', '/');
        String safe = normalized.substring(normalized.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        if (safe.isBlank() || safe.length() > 255) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filename must be at most 255 characters");
        return safe;
    }
    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0) throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "File extension is required");
        String ext = name.substring(dot).toLowerCase(Locale.ROOT);
        if (!List.of(".png", ".jpg", ".jpeg", ".gif", ".pdf", ".txt", ".csv", ".log").contains(ext))
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "File type is not allowed");
        return ext;
    }
    private boolean validContent(String ext, byte[] b) {
        if (ext.equals(".png")) return b.length > 8 && (b[0]&255)==137 && b[1]==80 && b[2]==78 && b[3]==71;
        if (ext.equals(".jpg") || ext.equals(".jpeg")) return b.length > 3 && (b[0]&255)==255 && (b[1]&255)==216 && (b[2]&255)==255;
        if (ext.equals(".gif")) return b.length > 6 && new String(b, 0, 6, StandardCharsets.US_ASCII).matches("GIF8[79]a");
        if (ext.equals(".pdf")) return b.length > 5 && new String(b, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-");
        for (byte value : b) {
            int c = value & 255;
            if (c == 0 || (c < 32 && c != 9 && c != 10 && c != 13)) return false;
        }
        return true;
    }
}
