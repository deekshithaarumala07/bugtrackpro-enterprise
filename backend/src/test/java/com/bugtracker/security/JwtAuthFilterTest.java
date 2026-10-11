package com.bugtracker.security;

import com.bugtracker.model.Role;
import com.bugtracker.model.User;
import com.bugtracker.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthFilterTest {
    private final JwtService jwt = mock(JwtService.class);
    private final UserRepository users = mock(UserRepository.class);
    private final JwtAuthFilter filter = new JwtAuthFilter(jwt, users);

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loadsCurrentActiveAccountAndRoleWithoutDependingOnUserService() throws Exception {
        Claims claims = Jwts.claims().subject("developer@example.test").build();
        User account = User.builder()
                .id(17L)
                .email("developer@example.test")
                .role(Role.DEVELOPER)
                .active(true)
                .build();
        when(jwt.parse("signed-token")).thenReturn(claims);
        when(users.findByEmail("developer@example.test")).thenReturn(Optional.of(account));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer signed-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> { };

        filter.doFilter(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("developer@example.test", authentication.getName());
        assertEquals("ROLE_DEVELOPER", authentication.getAuthorities().iterator().next().getAuthority());
        verify(users).findByEmail("developer@example.test");
    }
}
