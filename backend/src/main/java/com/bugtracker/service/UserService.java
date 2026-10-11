package com.bugtracker.service;

import com.bugtracker.model.Role;
import com.bugtracker.model.User;
import com.bugtracker.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public UserService(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = repo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .disabled(!user.isActive())
                .build();
    }

    public User find(Long id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User findEmail(String email) {
        return repo.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<User> all() {
        return repo.findAll();
    }

    public User save(User user) {
        return repo.save(user);
    }

    public User create(String name, String email, String password, Role role) {
        if (repo.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }
        return repo.save(User.builder()
                .name(name)
                .email(email)
                .password(encoder.encode(password))
                .role(role)
                .build());
    }
}
