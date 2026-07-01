package fr.miage.motus.stats.dto;

import java.time.LocalDateTime;

public record ResultLineDto(
        Long gameId,
        String status,
        boolean won,
        int attempts,
        int maxAttempts,
        int wordLength,
        String firstLetter,
        String secretWord,
        int score,
        LocalDateTime finishedAt
) {}
