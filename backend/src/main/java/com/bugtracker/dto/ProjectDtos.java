package com.bugtracker.dto;
import jakarta.validation.constraints.*;
public class ProjectDtos { public record CreateProjectRequest(@NotBlank @Size(max=255) String name,@Size(max=1000) String description,Long managerId){} }
