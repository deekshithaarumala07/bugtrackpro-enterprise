package com.bugtracker.security;

import com.bugtracker.model.User;
import com.bugtracker.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;

    public JwtAuthFilter(JwtService jwt, UserRepository users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                var claims = jwt.parse(authorization.substring(7));
                users.findByEmail(claims.getSubject())
                        .filter(User::isActive)
                        .ifPresent(user -> authenticate(user));
            } catch (JwtException | IllegalArgumentException ignored) {
                // Invalid, malformed, expired, or incorrectly signed tokens remain unauthenticated.
            }
        }
        chain.doFilter(request, response);
    }

    private void authenticate(User user) {
        var authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
