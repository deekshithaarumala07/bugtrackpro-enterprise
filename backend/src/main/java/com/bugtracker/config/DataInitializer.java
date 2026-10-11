
package com.bugtracker.config;

import com.bugtracker.model.Role;
import com.bugtracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seed(UserRepository users, com.bugtracker.service.UserService userService,
            @Value("${BOOTSTRAP_ADMIN_NAME:}") String name,
            @Value("${BOOTSTRAP_ADMIN_EMAIL:}") String email,
            @Value("${BOOTSTRAP_ADMIN_PASSWORD:}") String password) {
        return args -> {
            if (name.isBlank() || email.isBlank() || password.isBlank()
                    || users.countByRoleAndActiveTrue(Role.ADMIN) != 0) {
                return;
            }
            if (password.length() < 12) throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD must be at least 12 characters");
            userService.create(name, email, password, Role.ADMIN);
        };
    }
}
