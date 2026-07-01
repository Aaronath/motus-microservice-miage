package fr.miage.motus.game.dto;

import java.time.LocalDateTime;

public record GameSummaryDto(
        Long id, String status, int attemptsUsed, int maxAttempts,
        int wordLength, LocalDateTime startedAt, LocalDateTime finishedAt
) {}
