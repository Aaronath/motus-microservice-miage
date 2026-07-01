package fr.miage.motus.game.dto;

import java.time.LocalDateTime;
import java.util.List;

public record GameResponse(
        Long id,
        Long playerId,
        String playerPseudo,
        int wordLength,
        String firstLetter,
        int maxAttempts,
        int attemptsUsed,
        String status,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        String secretWord,
        List<GuessDto> guesses
) {}
