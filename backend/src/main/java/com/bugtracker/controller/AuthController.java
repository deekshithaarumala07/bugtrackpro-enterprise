package com.bugtracker.controller;

import com.bugtracker.dto.AuthDtos.AuthResponse;
import com.bugtracker.dto.AuthDtos.LoginRequest;
import com.bugtracker.dto.AuthDtos.RegisterRequest;
import com.bugtracker.model.AuditLog;
import com.bugtracker.model.Role;
import com.bugtracker.model.User;
import com.bugtracker.repository.AuditLogRepository;
import com.bugtracker.security.LoginAttemptLimiter;
import com.bugtracker.security.JwtService;
import com.bugtracker.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditLogRepository audits;
    private final LoginAttemptLimiter loginLimiter;

    public AuthController(UserService users, PasswordEncoder encoder, JwtService jwt, AuditLogRepository audits,
            LoginAttemptLimiter loginLimiter) {
        this.users = users; this.encoder = encoder; this.jwt = jwt; this.audits = audits;
        this.loginLimiter = loginLimiter;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String clientAddress = servletRequest.getRemoteAddr();
        loginLimiter.recordAttemptAndCheck(email, clientAddress);

        User user;
        try { user = users.findEmail(email); }
        catch (RuntimeException ex) {
            if ("User not found".equals(ex.getMessage())) return failedLogin(email, servletRequest);
            throw ex;
        }
        if (!user.isActive() || !encoder.matches(request.password(), user.getPassword()))
            return failedLogin(email, servletRequest);

        loginLimiter.clear(email, clientAddress);
        audit(email, "LOGIN_SUCCESS", servletRequest, "Successful login");
        return response(user);
    }

    private AuthResponse failedLogin(String email, HttpServletRequest request) {
        audit(email, "LOGIN_FAILED", request, "Failed login attempt");
        throw new BadCredentialsException("Invalid credentials");
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (!request.password().matches(".*[A-Z].*") || !request.password().matches(".*[0-9].*"))
            throw new IllegalArgumentException("Password must include an uppercase letter and a number");
        User user = users.create(request.name().trim(), request.email().trim().toLowerCase(Locale.ROOT), request.password(), Role.TESTER);
        return ResponseEntity.status(HttpStatus.CREATED).body(response(user));
    }

    private AuthResponse response(User user) {
        return new AuthResponse(jwt.generate(user.getEmail(), user.getRole().name(), user.getId()),
                user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    private void audit(String email, String action, HttpServletRequest request, String details) {
        audits.save(AuditLog.builder().actorEmail(email).action(action).ipAddress(request.getRemoteAddr()).details(details).build());
    }
}
