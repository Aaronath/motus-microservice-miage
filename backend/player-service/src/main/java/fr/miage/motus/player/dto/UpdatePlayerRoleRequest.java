package fr.miage.motus.player.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdatePlayerRoleRequest(@NotBlank String role) {}
