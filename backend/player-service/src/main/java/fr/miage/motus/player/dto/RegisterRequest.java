package fr.miage.motus.player.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) String pseudo,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, max = 100) String password,
        String adminCode
) {}
