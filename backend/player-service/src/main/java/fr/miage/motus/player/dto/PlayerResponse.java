package fr.miage.motus.player.dto;

import java.time.LocalDateTime;

public record PlayerResponse(Long id, String pseudo, String email, String role, LocalDateTime registeredAt) {}
