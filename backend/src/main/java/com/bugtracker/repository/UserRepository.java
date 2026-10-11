package com.bugtracker.repository;
import com.bugtracker.model.User; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface UserRepository extends JpaRepository<User,Long> { Optional<User> findByEmail(String email); long countByActiveTrue(); long countByRoleAndActiveTrue(com.bugtracker.model.Role role); }
