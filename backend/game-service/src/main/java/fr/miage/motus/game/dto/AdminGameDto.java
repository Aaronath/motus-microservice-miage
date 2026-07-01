package fr.miage.motus.game.dto;

import java.time.LocalDateTime;

public record AdminGameDto(
        Long id, Long playerId, String playerPseudo, String secretWord,
        String status, int attemptsUsed, int maxAttempts,
        LocalDateTime startedAt, LocalDateTime finishedAt
) {}
