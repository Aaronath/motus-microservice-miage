package fr.miage.motus.player.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String pseudo,
        @NotBlank String password
) {}
