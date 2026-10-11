package com.bugtracker.dto;
import jakarta.validation.constraints.*;
public class AuthDtos {
 public record LoginRequest(@Email @NotBlank String email,@NotBlank String password){}
 public record RegisterRequest(@NotBlank @Size(max=120) String name,@Email @NotBlank @Size(max=254) String email,@NotBlank @Size(min=12,max=128) String password){}
 public record AuthResponse(String token, Long id, String name, String email, String role){}
}
